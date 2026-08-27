export interface SemesterOption {
  label: string
  value: string
}

export function getCurrentSemesterValue(date = new Date()) {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  if (month >= 9) {
    return `${year}-${year + 1}-1`
  }
  return `${year - 1}-${year}-2`
}

export function formatSemesterLabel(value?: string) {
  if (!value) return ''
  const match = value.match(/^(\d{4})-(\d{4})-([12])$/)
  if (!match) return value
  return `${match[1]}-${match[2]} ${match[3] === '1' ? '第一学期' : '第二学期'}`
}

export function buildSemesterOptions(anchorDate = new Date(), beforeYears = 2, afterYears = 1): SemesterOption[] {
  const anchorYear = anchorDate.getFullYear()
  const options: SemesterOption[] = []
  for (let year = anchorYear - beforeYears; year <= anchorYear + afterYears; year++) {
    options.push({ label: `${year}-${year + 1} 第一学期`, value: `${year}-${year + 1}-1` })
    options.push({ label: `${year}-${year + 1} 第二学期`, value: `${year}-${year + 1}-2` })
  }
  return options
}

export function mergeSemesterOptions(base: SemesterOption[], values: Array<string | undefined | null>) {
  const optionMap = new Map(base.map(item => [item.value, item]))
  values
    .map(value => value?.trim())
    .filter((value): value is string => Boolean(value))
    .forEach(value => {
      if (!optionMap.has(value)) {
        optionMap.set(value, { value, label: formatSemesterLabel(value) || value })
      }
    })
  return Array.from(optionMap.values())
}
