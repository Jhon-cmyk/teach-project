<template>
  <div class="data-visual-page">
    <header class="analysis-header">
      <div>
        <h2>数据分析</h2>
        <p>导入成绩表，快速生成对比图和数据明细。</p>
      </div>
      <div class="header-actions">
        <a-button @click="downloadSampleWorkbook"><DownloadOutlined /> 下载样表</a-button>
        <a-button :disabled="!chartReady" @click="exportChartImage">
          <PictureOutlined /> 导出图片
        </a-button>
        <a-button v-if="headers.length" @click="resetAll"><ReloadOutlined /> 清空</a-button>
      </div>
    </header>

    <main class="visual-workspace">
      <section class="tool-card upload-card">
        <div class="card-head">
          <div>
            <h3><FileExcelOutlined /> 数据上传</h3>
            <p>支持 Excel 和 CSV，上传后自动读取第一行作为表头。</p>
          </div>
          <a-select
            v-if="sheetNames.length > 1"
            v-model:value="selectedSheetName"
            class="sheet-select"
            @change="handleSheetChange"
          >
            <a-select-option v-for="sheet in sheetNames" :key="sheet" :value="sheet">
              {{ sheet }}
            </a-select-option>
          </a-select>
        </div>

        <a-upload-dragger
          :multiple="false"
          :before-upload="handleUpload"
          :show-upload-list="false"
          accept=".xlsx,.xls,.csv"
          class="data-dropper"
        >
          <p class="upload-icon"><FileExcelOutlined /></p>
          <p class="upload-title">拖放文件到此处，或点击选择文件</p>
          <p class="upload-hint">支持 .xlsx、.xls、.csv 格式</p>
        </a-upload-dragger>

        <a-progress
          v-if="showProgress"
          :percent="progressPercentage"
          :stroke-width="6"
          class="upload-progress"
        />

        <div v-if="fileName" class="file-info">
          <div>
            <FileTextOutlined />
            <div>
              <strong>{{ fileName }}</strong>
              <span>{{ currentSheetName }} · {{ tableRows.length }} 行 · {{ headers.length }} 列</span>
            </div>
          </div>
          <a-button danger type="text" @click="resetAll"><DeleteOutlined /> 移除</a-button>
        </div>

        <div v-if="headers.length" class="preview-block">
          <div class="sub-head">
            <h4>数据预览</h4>
            <span v-if="hasMoreRows">仅显示前 10 行，还有 {{ tableRows.length - 10 }} 行未显示</span>
          </div>
          <a-table
            :dataSource="displayedData"
            :columns="tableColumns"
            :pagination="false"
            size="small"
            rowKey="__rowKey"
            bordered
            :scroll="{ x: tableScrollX }"
          />
        </div>
      </section>

      <section class="tool-card settings-card">
        <div class="card-head">
          <div>
            <h3><PieChartOutlined /> 可视化设置</h3>
            <p>选择图表类型、数据列和导出样式。</p>
          </div>
          <a-button type="primary" :disabled="!headers.length" @click="generateChart">
            <SyncOutlined /> 生成图表
          </a-button>
        </div>

        <div class="settings-grid">
          <div class="option-panel">
            <h4>图表类型</h4>
            <a-radio-group v-model:value="selectedChartType" class="stack-radio">
              <a-radio-button value="bar"><BarChartOutlined /> 柱状图</a-radio-button>
              <a-radio-button value="line"><LineChartOutlined /> 折线图</a-radio-button>
              <a-radio-button value="pie"><PieChartOutlined /> 饼图</a-radio-button>
              <a-radio-button value="doughnut"><AimOutlined /> 环形图</a-radio-button>
              <a-radio-button value="radar"><RadarChartOutlined /> 雷达图</a-radio-button>
            </a-radio-group>
          </div>

          <div class="option-panel">
            <h4>数据列选择</h4>
            <a-form layout="vertical">
              <a-form-item label="X轴 / 类别">
                <a-select v-model:value="selectedXAxis" placeholder="请选择" :disabled="!headers.length">
                  <a-select-option v-for="field in headers" :key="field" :value="field">
                    {{ field }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="Y轴 / 数值">
                <a-select v-model:value="selectedYAxis" placeholder="请选择" :disabled="!headers.length">
                  <a-select-option v-for="field in numericHeaders" :key="field" :value="field">
                    {{ field }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </div>

          <div class="option-panel wide-panel">
            <h4>图表选项</h4>
            <a-form layout="vertical">
              <a-form-item label="图表标题">
                <a-input v-model:value="chartTitle" placeholder="请输入图表标题" />
              </a-form-item>
              <a-form-item label="颜色主题">
                <a-radio-group v-model:value="selectedTheme" class="theme-picks">
                  <a-radio-button v-for="theme in themeOptions" :key="theme.value" :value="theme.value">
                    <span class="theme-dot" :style="{ background: theme.color }"></span>
                    {{ theme.label }}
                  </a-radio-button>
                </a-radio-group>
              </a-form-item>
              <div class="checkbox-line">
                <a-checkbox v-model:checked="showLegend">显示图例</a-checkbox>
                <a-checkbox v-model:checked="showGrid">显示网格线</a-checkbox>
              </div>
            </a-form>
          </div>
        </div>
      </section>

      <section class="tool-card chart-card">
        <div class="card-head">
          <div>
            <h3><PictureOutlined /> 图表展示</h3>
            <p>{{ chartReady ? `${selectedXAxis} / ${selectedYAxis}` : '上传数据并生成图表后将在此处显示' }}</p>
          </div>
        </div>
        <div v-if="!chartReady" class="chart-empty">
          <PieChartOutlined />
          <span>暂无图表</span>
        </div>
        <div ref="chartRef" class="chart-canvas" :class="{ hidden: !chartReady }"></div>
      </section>

      <section class="tool-card export-card">
        <div class="card-head">
          <div>
            <h3><DownloadOutlined /> 导出设置</h3>
            <p>可导出 PNG、JPG、SVG 和 PDF。</p>
          </div>
          <a-button type="primary" :disabled="!chartReady" @click="exportChart">
            <DownloadOutlined /> 导出图表
          </a-button>
        </div>

        <div class="export-grid">
          <div class="option-panel">
            <h4>导出格式</h4>
            <a-radio-group v-model:value="selectedExportFormat" class="stack-radio">
              <a-radio-button value="png"><PictureOutlined /> PNG 图片</a-radio-button>
              <a-radio-button value="jpg"><PictureOutlined /> JPG 图片</a-radio-button>
              <a-radio-button value="svg"><FileTextOutlined /> SVG 文件</a-radio-button>
              <a-radio-button value="pdf"><FilePdfOutlined /> PDF 文件</a-radio-button>
            </a-radio-group>
          </div>

          <div class="option-panel export-options">
            <h4>导出选项</h4>
            <a-form layout="vertical">
              <a-form-item label="文件名">
                <a-input v-model:value="exportFilename" placeholder="请输入文件名" />
              </a-form-item>
              <div class="size-row">
                <a-form-item label="宽度(px)">
                  <a-input-number v-model:value="exportWidth" :min="320" :max="3000" />
                </a-form-item>
                <a-form-item label="高度(px)">
                  <a-input-number v-model:value="exportHeight" :min="240" :max="2400" />
                </a-form-item>
              </div>
              <a-form-item label="背景">
                <a-radio-group v-model:value="exportBackground">
                  <a-radio :value="false">透明</a-radio>
                  <a-radio :value="true">白色</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-form>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import * as XLSX from 'xlsx'
import { message } from 'ant-design-vue'
import {
  AimOutlined,
  BarChartOutlined,
  DeleteOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  LineChartOutlined,
  PictureOutlined,
  PieChartOutlined,
  RadarChartOutlined,
  ReloadOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'

type DataRow = Record<string, string | number | null> & { __rowKey: string }
type ChartType = 'bar' | 'line' | 'pie' | 'doughnut' | 'radar'
type ExportFormat = 'png' | 'jpg' | 'svg' | 'pdf'

const fileName = ref('')
const headers = ref<string[]>([])
const tableRows = ref<DataRow[]>([])
const sheetNames = ref<string[]>([])
const selectedSheetName = ref('')
const selectedChartType = ref<ChartType>('bar')
const selectedXAxis = ref('')
const selectedYAxis = ref('')
const chartTitle = ref('')
const selectedTheme = ref('blue')
const showLegend = ref(true)
const showGrid = ref(true)
const showProgress = ref(false)
const progressPercentage = ref(0)
const chartReady = ref(false)

const selectedExportFormat = ref<ExportFormat>('png')
const exportFilename = ref('数据可视化图表')
const exportWidth = ref(1000)
const exportHeight = ref(640)
const exportBackground = ref(true)

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let workbookCache: XLSX.WorkBook | null = null
let progressTimer: number | null = null

const currentSheetName = computed(() => selectedSheetName.value || sheetNames.value[0] || '')
const displayedData = computed(() => tableRows.value.slice(0, 10))
const hasMoreRows = computed(() => tableRows.value.length > 10)
const tableScrollX = computed(() => Math.max(880, headers.value.length * 140))
const numericHeaders = computed(() => headers.value.filter((field) => numericValues(field).length > 0))

const tableColumns = computed(() =>
  headers.value.map((field) => ({
    title: field,
    dataIndex: field,
    key: field,
    width: 140,
    ellipsis: true,
  })),
)

const themeOptions = [
  { label: '蓝色', value: 'blue', color: '#409eff' },
  { label: '绿色', value: 'green', color: '#22c55e' },
  { label: '灰紫', value: 'purple', color: '#8b8f99' },
  { label: '红色', value: 'red', color: '#ef4444' },
  { label: '黄色', value: 'yellow', color: '#e6a23c' },
]

const themePalettes: Record<string, string[]> = {
  blue: ['#409eff', '#60a5fa', '#93c5fd', '#2563eb', '#38bdf8', '#1d4ed8'],
  green: ['#22c55e', '#16a34a', '#86efac', '#0f766e', '#34d399', '#15803d'],
  purple: ['#8b8f99', '#7c3aed', '#a78bfa', '#64748b', '#94a3b8', '#6d28d9'],
  red: ['#ef4444', '#f87171', '#fb7185', '#dc2626', '#fca5a5', '#b91c1c'],
  yellow: ['#e6a23c', '#f59e0b', '#fbbf24', '#d97706', '#fde68a', '#b45309'],
}

onMounted(() => {
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
  if (progressTimer) window.clearInterval(progressTimer)
})

function handleUpload(file: File) {
  const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase()
  if (!['.xlsx', '.xls', '.csv'].includes(ext)) {
    message.error('请上传 Excel 或 CSV 文件')
    return false
  }

  fileName.value = file.name
  showProgress.value = true
  progressPercentage.value = 0
  simulateProgress()

  const reader = new FileReader()
  reader.onload = (event) => {
    try {
      workbookCache = XLSX.read(event.target?.result, { type: 'array', cellDates: true })
      sheetNames.value = workbookCache.SheetNames || []
      if (!sheetNames.value.length) {
        message.error('文件中没有可读取的工作表')
        resetAll()
        return
      }
      selectedSheetName.value = sheetNames.value[0]
      applySheetData(selectedSheetName.value)
      progressPercentage.value = 100
      window.setTimeout(() => {
        showProgress.value = false
      }, 260)
      message.success('文件上传成功')
    } catch (error) {
      console.error(error)
      message.error('文件解析失败，请确认文件格式正确')
      showProgress.value = false
    }
  }
  reader.onerror = () => {
    message.error('读取文件时出错')
    showProgress.value = false
  }
  reader.readAsArrayBuffer(file)
  return false
}

function simulateProgress() {
  if (progressTimer) window.clearInterval(progressTimer)
  progressTimer = window.setInterval(() => {
    if (!showProgress.value) {
      if (progressTimer) window.clearInterval(progressTimer)
      progressTimer = null
      return
    }
    progressPercentage.value = Math.min(96, progressPercentage.value + 12)
  }, 160)
}

function handleSheetChange(sheetName: string) {
  applySheetData(sheetName)
}

function applySheetData(sheetName: string) {
  if (!workbookCache) return
  const worksheet = workbookCache.Sheets[sheetName]
  const matrix = XLSX.utils.sheet_to_json<unknown[]>(worksheet, {
    header: 1,
    defval: '',
    raw: true,
    blankrows: false,
  })
  const parsed = parseMatrix(matrix, sheetName)
  headers.value = parsed.headers
  tableRows.value = parsed.rows
  inferColumns()
  chartReady.value = false
  chartInstance?.clear()
}

function parseMatrix(matrix: unknown[][], sheetName: string) {
  const headerRowIndex = matrix.findIndex((row) => row.filter((cell) => valueText(cell)).length >= 2)
  if (headerRowIndex < 0) return { headers: [], rows: [] as DataRow[] }

  const parsedHeaders = normalizeHeaders(matrix[headerRowIndex].map(valueText))
  const rows = matrix
    .slice(headerRowIndex + 1)
    .map((line, rowIndex) => {
      const row = parsedHeaders.reduce<DataRow>(
        (acc, header, colIndex) => {
          acc[header] = normalizeCell(line[colIndex])
          return acc
        },
        { __rowKey: `${sheetName}-${rowIndex}` },
      )
      return row
    })
    .filter((row) => parsedHeaders.some((header) => valueText(row[header])))

  return { headers: parsedHeaders, rows }
}

function normalizeHeaders(rawHeaders: string[]) {
  const seen = new Map<string, number>()
  return rawHeaders.map((header, index) => {
    const base = header || `未命名字段${index + 1}`
    const count = seen.get(base) || 0
    seen.set(base, count + 1)
    return count ? `${base}_${count + 1}` : base
  })
}

function inferColumns() {
  selectedXAxis.value =
    headers.value.find((field) => /班级|类别|姓名|专业|课程|名称/.test(field)) || headers.value[0] || ''
  selectedYAxis.value =
    numericHeaders.value.find((field) => /成绩|分数|得分|平均|总分|绩点/.test(field)) ||
    numericHeaders.value[0] ||
    ''
  chartTitle.value = selectedYAxis.value ? `${selectedYAxis.value} 图表` : ''
}

function generateChart() {
  if (!headers.value.length) {
    message.error('请先上传数据文件')
    return
  }
  if (!selectedXAxis.value || !selectedYAxis.value) {
    message.error('请选择 X轴 和 Y轴 数据列')
    return
  }
  if (selectedXAxis.value === selectedYAxis.value) {
    message.error('X轴和Y轴不能选择同一列')
    return
  }

  const chartData = buildChartData()
  if (!chartData.length) {
    message.error('所选列中没有有效数据')
    return
  }

  chartReady.value = true
  nextTick(() => {
    renderChart(chartData)
    message.success('图表生成成功')
  })
}

function buildChartData() {
  const groups = new Map<string, number[]>()
  tableRows.value.forEach((row) => {
    const label = valueText(row[selectedXAxis.value])
    const value = toNumber(row[selectedYAxis.value])
    if (!label || value === null) return
    if (!groups.has(label)) groups.set(label, [])
    groups.get(label)!.push(value)
  })
  return Array.from(groups.entries()).map(([label, values]) => ({
    label,
    value: values.reduce((sum, item) => sum + item, 0) / values.length,
  }))
}

function renderChart(chartData = buildChartData()) {
  if (!chartRef.value || !chartData.length) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const labels = chartData.map((item) => item.label)
  const values = chartData.map((item) => Number(item.value.toFixed(2)))
  const colors = themePalettes[selectedTheme.value] || themePalettes.blue
  const title = chartTitle.value || `${selectedYAxis.value} 图表`
  const format = selectedChartType.value

  const baseOption: echarts.EChartsOption = {
    color: colors,
    title: {
      text: title,
      left: 'center',
      top: 8,
      textStyle: { color: '#1f2937', fontSize: 16, fontWeight: 700 },
    },
    tooltip: {
      trigger: format === 'pie' || format === 'doughnut' ? 'item' : 'axis',
    },
    legend: {
      show: showLegend.value,
      top: 40,
      type: 'scroll',
    },
    animationDuration: 500,
  }

  let option: echarts.EChartsOption
  if (format === 'pie' || format === 'doughnut') {
    option = {
      ...baseOption,
      series: [
        {
          name: selectedYAxis.value,
          type: 'pie',
          radius: format === 'doughnut' ? ['45%', '68%'] : '66%',
          center: ['50%', '56%'],
          label: { formatter: '{b}\n{d}%' },
          data: chartData.map((item) => ({ name: item.label, value: Number(item.value.toFixed(2)) })),
        },
      ],
    }
  } else if (format === 'radar') {
    const topData = chartData.slice(0, 8)
    const maxValue = Math.max(...topData.map((item) => item.value), 1)
    option = {
      ...baseOption,
      radar: {
        center: ['50%', '58%'],
        radius: '58%',
        indicator: topData.map((item) => ({
          name: item.label,
          max: Math.ceil(maxValue * 1.2),
        })),
      },
      series: [
        {
          name: selectedYAxis.value,
          type: 'radar',
          areaStyle: { opacity: 0.18 },
          data: [{ value: topData.map((item) => Number(item.value.toFixed(2))), name: selectedYAxis.value }],
        },
      ],
    }
  } else {
    option = {
      ...baseOption,
      grid: {
        left: 52,
        right: 30,
        top: showLegend.value ? 82 : 58,
        bottom: labels.length > 8 ? 88 : 48,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { interval: 0, rotate: labels.length > 8 ? 32 : 0, color: '#64748b' },
        axisLine: { lineStyle: { color: '#d6dee9' } },
      },
      yAxis: {
        type: 'value',
        splitLine: { show: showGrid.value, lineStyle: { color: '#e8edf5', type: 'dashed' } },
        axisLabel: { color: '#64748b' },
      },
      series: [
        {
          name: selectedYAxis.value,
          type: format,
          data: values,
          smooth: format === 'line',
          symbolSize: 8,
          barMaxWidth: 42,
          label: { show: format === 'bar', position: 'top', color: '#475569' },
          itemStyle: { borderRadius: format === 'bar' ? [6, 6, 0, 0] : 0 },
          areaStyle: format === 'line' ? { opacity: 0.08 } : undefined,
        },
      ],
    }
  }

  chartInstance.setOption(option, true)
}

function exportChartImage() {
  selectedExportFormat.value = 'png'
  exportChart()
}

function exportChart() {
  if (!chartInstance || !chartReady.value) {
    message.error('请先生成图表')
    return
  }

  const filename = safeFilename(exportFilename.value || '数据可视化图表')
  const backgroundColor = exportBackground.value ? '#ffffff' : 'transparent'
  const imageType = selectedExportFormat.value === 'jpg' ? 'jpeg' : 'png'
  const dataUrl = chartInstance.getDataURL({
    type: imageType,
    pixelRatio: Math.max(1, Math.round(exportWidth.value / 500)),
    backgroundColor,
  })

  if (selectedExportFormat.value === 'png') {
    downloadDataUrl(dataUrl, `${filename}.png`)
  } else if (selectedExportFormat.value === 'jpg') {
    downloadDataUrl(dataUrl, `${filename}.jpg`)
  } else if (selectedExportFormat.value === 'svg') {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${exportWidth.value}" height="${exportHeight.value}" viewBox="0 0 ${exportWidth.value} ${exportHeight.value}"><rect width="100%" height="100%" fill="${backgroundColor}"/><image href="${dataUrl}" width="${exportWidth.value}" height="${exportHeight.value}" preserveAspectRatio="xMidYMid meet"/></svg>`
    downloadBlob(svg, `${filename}.svg`, 'image/svg+xml;charset=utf-8')
  } else {
    openPdfPrintWindow(dataUrl, filename)
  }

  message.success(`图表已导出为 ${filename}.${selectedExportFormat.value}`)
}

function openPdfPrintWindow(dataUrl: string, filename: string) {
  const popup = window.open('', '_blank')
  if (!popup) {
    message.warning('浏览器拦截了导出窗口，请允许弹窗后重试')
    return
  }
  popup.document.write(`
    <html>
      <head><title>${filename}</title></head>
      <body style="margin:0;display:flex;align-items:center;justify-content:center;background:#fff;">
        <img src="${dataUrl}" style="max-width:100vw;max-height:100vh;width:${exportWidth.value}px;height:auto;" />
        <script>window.onload = () => window.print();<\/script>
      </body>
    </html>
  `)
  popup.document.close()
}

function downloadSampleWorkbook() {
  const sampleRows = [
    { 学号: '20260001', 姓名: '张同学', 班级: '计科一班', 专业: '计算机科学与技术', 成绩: 92 },
    { 学号: '20260002', 姓名: '李同学', 班级: '计科一班', 专业: '计算机科学与技术', 成绩: 85 },
    { 学号: '20260003', 姓名: '王同学', 班级: '计科二班', 专业: '计算机科学与技术', 成绩: 78 },
    { 学号: '20260004', 姓名: '赵同学', 班级: '软工一班', 专业: '软件工程', 成绩: 88 },
    { 学号: '20260005', 姓名: '陈同学', 班级: '软工一班', 专业: '软件工程', 成绩: 64 },
  ]
  const worksheet = XLSX.utils.json_to_sheet(sampleRows)
  worksheet['!cols'] = [{ wch: 14 }, { wch: 12 }, { wch: 14 }, { wch: 22 }, { wch: 10 }]
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '成绩样表')
  XLSX.writeFile(workbook, '成绩数据导入样表.xlsx')
  message.success('样表已下载')
}

function resetAll() {
  fileName.value = ''
  headers.value = []
  tableRows.value = []
  sheetNames.value = []
  selectedSheetName.value = ''
  selectedXAxis.value = ''
  selectedYAxis.value = ''
  chartTitle.value = ''
  showProgress.value = false
  progressPercentage.value = 0
  chartReady.value = false
  workbookCache = null
  chartInstance?.clear()
}

function resizeChart() {
  chartInstance?.resize()
}

function numericValues(field: string) {
  return tableRows.value.map((row) => toNumber(row[field])).filter((value): value is number => value !== null)
}

function normalizeCell(value: unknown) {
  if (value instanceof Date) return value.toLocaleDateString()
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function toNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value !== 'string') return null
  const normalized = value.replace(/,/g, '').replace(/%$/, '').trim()
  if (!normalized) return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function valueText(value: unknown) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function safeFilename(value: string) {
  return value.replace(/[\\/:*?"<>|]/g, '-').trim() || '数据可视化图表'
}

function downloadDataUrl(dataUrl: string, filename: string) {
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = filename
  link.click()
}

function downloadBlob(content: string, filename: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  downloadDataUrl(url, filename)
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.data-visual-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #172033;
}

.analysis-header {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 0 0 12px;
  flex-shrink: 0;
}

.analysis-header h2 {
  margin: 0;
  color: #172033;
  font-size: 24px;
  line-height: 1.25;
  font-weight: 800;
}

.analysis-header p {
  margin: 5px 0 0;
  color: #708096;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.visual-workspace {
  flex: 1;
  width: 100%;
  min-height: 0;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: block;
  padding: 18px;
  border: 1px solid #dde6f0;
  border-radius: 7px;
  background: #f7f9fc;
  box-sizing: border-box;
}

.tool-card {
  width: 100%;
  min-width: 0;
  padding: 18px;
  border: 1px solid #e1e8f0;
  border-radius: 7px;
  background: #fff;
  overflow: visible;
  box-sizing: border-box;
}

.tool-card + .tool-card {
  margin-top: 16px;
}

.card-head,
.sub-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.card-head h3,
.sub-head h4,
.option-panel h4 {
  margin: 0;
  color: #223247;
  font-size: 16px;
  font-weight: 800;
}

.card-head h3 {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-head p,
.sub-head span {
  margin: 5px 0 0;
  color: #708096;
  font-size: 13px;
}

.sheet-select {
  width: 180px;
}

.data-dropper {
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.data-dropper :deep(.ant-upload) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.data-dropper :deep(.ant-upload-drag) {
  width: 100%;
  max-width: 100%;
  min-height: 190px;
  box-sizing: border-box;
  border-color: #ccd6e2;
  border-radius: 7px;
  background: #fbfcfe;
}

.data-dropper :deep(.ant-upload-drag-container) {
  min-height: 190px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.upload-icon {
  margin: 8px 0 6px;
  color: #409eff;
  font-size: 46px;
}

.upload-title {
  margin: 0;
  color: #475569;
  font-size: 15px;
  font-weight: 700;
}

.upload-hint {
  margin: 8px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.upload-progress {
  margin-top: 14px;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #e3ebf4;
  border-radius: 7px;
  background: #fbfdff;
}

.file-info > div {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.file-info .anticon {
  color: #409eff;
  font-size: 18px;
}

.file-info strong,
.file-info span {
  display: block;
}

.file-info strong {
  color: #24344a;
  font-size: 14px;
}

.file-info span {
  margin-top: 2px;
  color: #708096;
  font-size: 12px;
}

.preview-block {
  min-width: 0;
  max-width: 100%;
  margin-top: 16px;
  overflow: hidden;
}

.preview-block :deep(.ant-table-wrapper) {
  max-width: 100%;
}

.settings-grid,
.export-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(180px, 0.7fr) minmax(220px, 0.8fr) minmax(360px, 1.45fr);
  gap: 14px;
}

.export-grid {
  grid-template-columns: minmax(220px, 0.75fr) minmax(420px, 1.7fr);
}

.option-panel {
  min-width: 0;
  padding: 15px;
  border: 1px solid #e5ebf2;
  border-radius: 7px;
  background: #fbfcfe;
}

.option-panel h4 {
  margin-bottom: 12px;
  font-size: 14px;
}

.stack-radio {
  width: 100%;
  display: grid;
  gap: 10px;
}

.stack-radio :deep(.ant-radio-button-wrapper) {
  width: 100%;
  height: 38px;
  border-inline-start-width: 1px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 700;
}

.stack-radio :deep(.ant-radio-button-wrapper::before) {
  display: none;
}

.theme-picks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.theme-picks :deep(.ant-radio-button-wrapper) {
  border-inline-start-width: 1px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.theme-picks :deep(.ant-radio-button-wrapper::before) {
  display: none;
}

.theme-dot {
  width: 14px;
  height: 14px;
  display: inline-block;
  border-radius: 4px;
}

.checkbox-line {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}

.chart-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: visible;
}

.chart-empty {
  width: 100%;
  min-width: 0;
  height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 7px;
  color: #9aa7b8;
  background: #fbfcfe;
  box-sizing: border-box;
}

.chart-empty .anticon {
  font-size: 46px;
}

.chart-canvas {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 400px;
  overflow: hidden;
  box-sizing: border-box;
}

.chart-canvas.hidden {
  height: 0;
  overflow: hidden;
}

.size-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.size-row :deep(.ant-input-number) {
  width: 100%;
}

:deep(.ant-btn),
:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-select-selector),
:deep(.ant-radio-button-wrapper) {
  border-radius: 6px !important;
}

:deep(.ant-form-item-label > label) {
  color: #53647a;
  font-weight: 700;
}

:deep(.ant-table-thead > tr > th) {
  background: #f7f9fc;
  color: #42536a;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .settings-grid,
  .export-grid {
    grid-template-columns: 1fr 1fr;
  }

  .wide-panel,
  .export-options {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .analysis-header,
  .card-head {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .settings-grid,
  .export-grid,
  .size-row {
    grid-template-columns: 1fr;
  }
}
</style>
