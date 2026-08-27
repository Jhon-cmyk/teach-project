package com.ruyi.teach.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruyi.teach.common.BaseResponse;
import com.ruyi.teach.common.ResultUtils;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import com.ruyi.teach.mapper.AdminImportBatchMapper;
import com.ruyi.teach.mapper.SysClassMapper;
import com.ruyi.teach.model.entity.AdminImportBatch;
import com.ruyi.teach.model.entity.SysClass;
import com.ruyi.teach.model.entity.User;
import com.ruyi.teach.service.AdminAuditLogger;
import com.ruyi.teach.service.PasswordService;
import com.ruyi.teach.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/data-transfer")
public class AdminDataTransferController {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BACKUP_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Resource
    private UserService userService;

    @Resource
    private PasswordService passwordService;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private AdminAuditLogger adminAuditLogger;

    @Resource
    private AdminImportBatchMapper adminImportBatchMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DataSource dataSource;

    @Value("${ruyi.upload-path:D:/teach/files/}")
    private String uploadPath;

    @GetMapping("/template/students")
    public void downloadStudentTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("学生导入模板");
            writeRow(sheet, 0, "学号", "姓名", "专业", "班级", "学院");
            writeRow(sheet, 1, "202612340001", "张三", "软件工程", "25级软件工程1班", "计算机学院");
            writeRow(sheet, 2, "202612340002", "李四", "人工智能", "25级人工智能1班", "人工智能学院");
            Sheet note = workbook.createSheet("填写说明");
            writeRow(note, 0, "说明项", "内容");
            writeRow(note, 1, "账号规则", "账号就是学号，默认密码为学号后 8 位。");
            writeRow(note, 2, "班级规则", "如果班级不存在，导入学生时会自动创建班级。");
            writeRow(note, 3, "重复规则", "学号已存在时会跳过，不覆盖原账号。");
            writeWorkbook(response, workbook, "学生账号导入模板.xlsx");
        }
    }

    @GetMapping("/template/classes")
    public void downloadClassTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("班级专业模板");
            writeRow(sheet, 0, "班级名称", "专业", "学院");
            writeRow(sheet, 1, "25级软件工程1班", "软件工程", "计算机学院");
            writeRow(sheet, 2, "25级人工智能1班", "人工智能", "人工智能学院");
            Sheet note = workbook.createSheet("填写说明");
            writeRow(note, 0, "说明项", "内容");
            writeRow(note, 1, "重复规则", "班级名称已存在时会跳过。");
            writeRow(note, 2, "导入建议", "建议先导入班级专业，再导入学生账号。");
            writeWorkbook(response, workbook, "班级专业导入模板.xlsx");
        }
    }

    @GetMapping("/export/users")
    public void exportUsers(HttpServletRequest request, HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        List<User> users = userService.list(new QueryWrapper<User>().orderByDesc("createTime"));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("用户账号");
            writeRow(sheet, 0, "账号", "姓名", "角色", "班级ID", "教师职称", "教师注册号", "创建时间");
            int rowIndex = 1;
            for (User user : users) {
                writeRow(sheet, rowIndex++,
                        user.getUserAccount(),
                        user.getUserName(),
                        user.getUserRole(),
                        user.getClassId() == null ? "" : String.valueOf(user.getClassId()),
                        user.getTeacherTitle(),
                        user.getTeacherRegisterCode(),
                        user.getCreateTime() == null ? "" : user.getCreateTime().toString());
            }
            writeWorkbook(response, workbook, "用户账号导出.xlsx");
        }
    }

    @GetMapping("/export/classes")
    public void exportClasses(HttpServletRequest request, HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        List<SysClass> classes = sysClassMapper.selectAllClassesWithStudentCount();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("班级专业");
            writeRow(sheet, 0, "班级名称", "专业", "学院", "学生数量", "创建时间");
            int rowIndex = 1;
            for (SysClass item : classes) {
                writeRow(sheet, rowIndex++,
                        item.getName(),
                        item.getMajor(),
                        item.getCollege(),
                        item.getStudentCount() == null ? "0" : String.valueOf(item.getStudentCount()),
                        item.getCreateTime() == null ? "" : item.getCreateTime().toString());
            }
            writeWorkbook(response, workbook, "班级专业导出.xlsx");
        }
    }

    @PostMapping("/import/students")
    public BaseResponse<ImportResultVO> importStudents(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request) throws Exception {
        User admin = getAdminLoginUser(request);
        assertExcelFile(file);

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, 5)) {
                    continue;
                }
                String account = cell(row, 0);
                String name = cell(row, 1);
                String major = cell(row, 2);
                String className = cell(row, 3);
                String college = cell(row, 4);
                if (StringUtils.isBlank(account)) {
                    skipped++;
                    errors.add("第 " + (i + 1) + " 行缺少学号");
                    continue;
                }
                if (userService.count(new QueryWrapper<User>().eq("userAccount", account.trim())) > 0) {
                    skipped++;
                    continue;
                }

                Long classId = null;
                if (StringUtils.isNotBlank(className)) {
                    classId = findOrCreateClass(className.trim(), major, college);
                }

                User user = new User();
                user.setUserAccount(account.trim());
                user.setUserName(StringUtils.defaultIfBlank(name, account.trim()));
                user.setUserRole("student");
                user.setClassId(classId);
                String rawPassword = account.trim().length() <= 8
                        ? account.trim()
                        : account.trim().substring(account.trim().length() - 8);
                user.setUserPassword(passwordService.encode(rawPassword));
                user.setCreateTime(new Date());
                user.setUpdateTime(new Date());
                user.setIsDelete(0);
                userService.save(user);
                created++;
            }
        }

        ImportResultVO result = new ImportResultVO();
        result.setCreated(created);
        result.setSkipped(skipped);
        result.setErrors(errors);
        AdminImportBatch batch = saveImportBatch("students", file, result, admin, request);
        result.setBatchId(batch.getId());
        adminAuditLogger.log(admin, "导入导出中心", "导入学生账号", "user", "",
                "新增 " + created + " 个学生，跳过 " + skipped + " 行", request);
        return ResultUtils.success(result);
    }

    @PostMapping("/import/classes")
    public BaseResponse<ImportResultVO> importClasses(@RequestParam("file") MultipartFile file,
                                                      HttpServletRequest request) throws Exception {
        User admin = getAdminLoginUser(request);
        assertExcelFile(file);

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, 3)) {
                    continue;
                }
                String name = cell(row, 0);
                if (StringUtils.isBlank(name)) {
                    skipped++;
                    errors.add("第 " + (i + 1) + " 行缺少班级名称");
                    continue;
                }
                long exists = sysClassMapper.selectCount(new QueryWrapper<SysClass>().eq("name", name.trim()));
                if (exists > 0) {
                    skipped++;
                    continue;
                }
                SysClass sysClass = new SysClass();
                sysClass.setName(name.trim());
                sysClass.setMajor(cell(row, 1));
                sysClass.setCollege(cell(row, 2));
                sysClass.setCreateTime(new Date());
                sysClassMapper.insert(sysClass);
                created++;
            }
        }

        ImportResultVO result = new ImportResultVO();
        result.setCreated(created);
        result.setSkipped(skipped);
        result.setErrors(errors);
        AdminImportBatch batch = saveImportBatch("classes", file, result, admin, request);
        result.setBatchId(batch.getId());
        adminAuditLogger.log(admin, "导入导出中心", "导入班级专业", "sys_class", "",
                "新增 " + created + " 个班级，跳过 " + skipped + " 行", request);
        return ResultUtils.success(result);
    }

    @GetMapping("/import-batches")
    public BaseResponse<Page<AdminImportBatch>> listImportBatches(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String importType,
            HttpServletRequest request) {
        getAdminLoginUser(request);
        QueryWrapper<AdminImportBatch> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(importType)) {
            wrapper.eq("import_type", importType.trim());
        }
        wrapper.orderByDesc("create_time");
        return ResultUtils.success(adminImportBatchMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @GetMapping("/import-batches/error-report")
    public void downloadImportErrorReport(@RequestParam Long id,
                                          HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        AdminImportBatch batch = adminImportBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "导入批次不存在");
        }

        List<String> errors = parseErrorList(batch.getErrorJson());
        response.setContentType("text/csv; charset=UTF-8");
        String filename = "导入错误报告_" + batch.getId() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8));
        StringBuilder csv = new StringBuilder("\uFEFF序号,错误信息\n");
        for (int i = 0; i < errors.size(); i++) {
            csv.append(i + 1)
                    .append(",\"")
                    .append(errors.get(i).replace("\"", "\"\""))
                    .append("\"\n");
        }
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/backup/status")
    public BaseResponse<BackupStatusVO> backupStatus(HttpServletRequest request) throws IOException {
        getAdminLoginUser(request);
        List<BackupFileVO> backups = listBackupFiles();
        BackupStatusVO vo = new BackupStatusVO();
        vo.setBackupDir(getBackupDir().toString());
        vo.setBackupCount(backups.size());
        vo.setLatestBackup(backups.isEmpty() ? null : backups.get(0));
        vo.setRestoreEnabled(true);
        return ResultUtils.success(vo);
    }

    @GetMapping("/backup/list")
    public BaseResponse<List<BackupFileVO>> listBackups(HttpServletRequest request) throws IOException {
        getAdminLoginUser(request);
        return ResultUtils.success(listBackupFiles());
    }

    @PostMapping("/backup/create")
    public BaseResponse<BackupFileVO> createBackup(HttpServletRequest request) throws Exception {
        User admin = getAdminLoginUser(request);
        Path backupDir = getBackupDir();
        Files.createDirectories(backupDir);

        String filename = "teach_platform_backup_" + LocalDateTime.now(BEIJING_ZONE).format(BACKUP_NAME_FORMAT) + ".sql";
        Path backupFile = backupDir.resolve(filename);
        String sql = buildDatabaseDump();
        Files.writeString(backupFile, sql, StandardCharsets.UTF_8);

        BackupFileVO vo = toBackupFileVO(backupFile);
        adminAuditLogger.log(admin, "导入导出中心", "创建数据库备份", "database_backup", filename,
                "大小 " + vo.getSizeText(), request);
        return ResultUtils.success(vo);
    }

    @GetMapping("/backup/download")
    public void downloadBackup(@RequestParam String filename,
                               HttpServletRequest request,
                               HttpServletResponse response) throws Exception {
        getAdminLoginUser(request);
        Path file = resolveBackupFile(filename);
        if (!Files.exists(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "备份文件不存在");
        }
        response.setContentType("application/sql; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8));
        Files.copy(file, response.getOutputStream());
    }

    @PostMapping("/backup/restore")
    public BaseResponse<Boolean> restoreBackup(@RequestBody RestoreBackupRequest requestBody,
                                               HttpServletRequest request) throws Exception {
        User admin = getAdminLoginUser(request);
        if (requestBody == null || StringUtils.isBlank(requestBody.getFilename())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要恢复的备份文件");
        }

        Path file = resolveBackupFile(requestBody.getFilename());
        if (!Files.exists(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "备份文件不存在");
        }

        String sql = Files.readString(file, StandardCharsets.UTF_8);
        executeSqlScript(sql);
        adminAuditLogger.log(admin, "导入导出中心", "恢复数据库备份", "database_backup", file.getFileName(),
                "已执行备份恢复", request);
        return ResultUtils.success(true);
    }

    private void assertExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传 Excel 文件");
        }
        String name = StringUtils.defaultString(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 .xlsx 或 .xls 文件");
        }
    }

    private AdminImportBatch saveImportBatch(String importType,
                                             MultipartFile file,
                                             ImportResultVO result,
                                             User admin,
                                             HttpServletRequest request) throws Exception {
        int errorCount = result.getErrors() == null ? 0 : result.getErrors().size();
        AdminImportBatch batch = new AdminImportBatch();
        batch.setImportType(importType);
        batch.setFileName(StringUtils.defaultIfBlank(file.getOriginalFilename(), "unknown.xlsx"));
        batch.setCreatedCount(result.getCreated());
        batch.setSkippedCount(result.getSkipped());
        batch.setErrorCount(errorCount);
        batch.setErrorJson(objectMapper.writeValueAsString(result.getErrors() == null ? List.of() : result.getErrors()));
        batch.setStatus(errorCount == 0 ? "success" : (result.getCreated() != null && result.getCreated() > 0 ? "partial" : "failed"));
        batch.setAdminId(admin.getId());
        batch.setAdminAccount(admin.getUserAccount());
        batch.setAdminName(StringUtils.defaultIfBlank(admin.getUserName(), admin.getUserAccount()));
        batch.setRequestIp(resolveIp(request));
        batch.setCreateTime(LocalDateTime.now(BEIJING_ZONE));
        adminImportBatchMapper.insert(batch);
        return batch;
    }

    private List<String> parseErrorList(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.isNotBlank(value)) {
                String first = value.split(",")[0].trim();
                if (StringUtils.isNotBlank(first) && !"unknown".equalsIgnoreCase(first)) {
                    return normalizeLocalIp(first);
                }
            }
        }
        return normalizeLocalIp(StringUtils.defaultString(request.getRemoteAddr()));
    }

    private String normalizeLocalIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private Long findOrCreateClass(String className, String major, String college) {
        SysClass exist = sysClassMapper.selectOne(new QueryWrapper<SysClass>().eq("name", className).last("limit 1"));
        if (exist != null) {
            return exist.getId();
        }
        SysClass sysClass = new SysClass();
        sysClass.setName(className);
        sysClass.setMajor(StringUtils.trimToEmpty(major));
        sysClass.setCollege(StringUtils.trimToEmpty(college));
        sysClass.setCreateTime(new Date());
        sysClassMapper.insert(sysClass);
        return sysClass.getId();
    }

    private boolean isBlankRow(Row row, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            if (StringUtils.isNotBlank(cell(row, i))) {
                return false;
            }
        }
        return true;
    }

    private void writeRow(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(StringUtils.defaultString(values[i]));
            sheet.setColumnWidth(i, Math.max(14, Math.min(28, StringUtils.defaultString(values[i]).length() + 8)) * 256);
        }
    }

    private String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return StringUtils.trimToEmpty(cell.getStringCellValue());
    }

    private void writeWorkbook(HttpServletResponse response, Workbook workbook, String filename) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
    }

    private Path getBackupDir() {
        return Path.of(uploadPath).resolve("admin-backups").normalize();
    }

    private Path resolveBackupFile(String filename) {
        String cleanName = Path.of(filename).getFileName().toString();
        if (!cleanName.endsWith(".sql")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "备份文件格式不正确");
        }
        Path backupDir = getBackupDir();
        Path file = backupDir.resolve(cleanName).normalize();
        if (!file.startsWith(backupDir)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "备份文件路径不正确");
        }
        return file;
    }

    private List<BackupFileVO> listBackupFiles() throws IOException {
        Path backupDir = getBackupDir();
        if (!Files.exists(backupDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(backupDir)) {
            return stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                    .map(this::toBackupFileVO)
                    .toList();
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private BackupFileVO toBackupFileVO(Path path) {
        try {
            BackupFileVO vo = new BackupFileVO();
            vo.setFilename(path.getFileName().toString());
            vo.setSize(Files.size(path));
            vo.setSizeText(formatSize(vo.getSize()));
            vo.setCreateTime(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), BEIJING_ZONE)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return vo;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取备份文件失败");
        }
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", size / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", size / 1024.0 / 1024.0);
    }

    private String buildDatabaseDump() throws SQLException {
        StringBuilder builder = new StringBuilder();
        try (Connection connection = dataSource.getConnection()) {
            String catalog = connection.getCatalog();
            builder.append("-- Teach platform database backup\n")
                    .append("-- Database: ").append(catalog).append('\n')
                    .append("-- Created at: ").append(LocalDateTime.now(BEIJING_ZONE)).append("\n\n")
                    .append("SET FOREIGN_KEY_CHECKS=0;\n\n");

            List<String> tableNames = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }
            }
            tableNames.sort(String::compareToIgnoreCase);

            try (Statement statement = connection.createStatement()) {
                for (String tableName : tableNames) {
                    appendTableDump(builder, statement, tableName);
                }
            }
            builder.append("SET FOREIGN_KEY_CHECKS=1;\n");
        }
        return builder.toString();
    }

    private void appendTableDump(StringBuilder builder, Statement statement, String tableName) throws SQLException {
        builder.append("\n-- ----------------------------\n")
                .append("-- Table structure for `").append(tableName).append("`\n")
                .append("-- ----------------------------\n")
                .append("DROP TABLE IF EXISTS `").append(tableName).append("`;\n");

        try (ResultSet createTable = statement.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
            if (createTable.next()) {
                builder.append(createTable.getString(2)).append(";\n\n");
            }
        }

        try (ResultSet rows = statement.executeQuery("SELECT * FROM `" + tableName + "`")) {
            ResultSetMetaData metaData = rows.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rows.next()) {
                builder.append("INSERT INTO `").append(tableName).append("` (");
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        builder.append(", ");
                    }
                    builder.append('`').append(metaData.getColumnName(i)).append('`');
                }
                builder.append(") VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        builder.append(", ");
                    }
                    builder.append(toSqlLiteral(rows.getObject(i)));
                }
                builder.append(");\n");
            }
        }
    }

    private String toSqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number && !(value instanceof BigDecimal)) {
            return value.toString();
        }
        String text = String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "'" + text + "'";
    }

    private void executeSqlScript(String sql) throws SQLException {
        List<String> statements = splitSqlStatements(stripSqlComments(sql));
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String item : statements) {
                String trimmed = item.trim();
                if (StringUtils.isBlank(trimmed) || trimmed.startsWith("--")) {
                    continue;
                }
                statement.execute(trimmed);
            }
        }
    }

    private String stripSqlComments(String sql) {
        StringBuilder builder = new StringBuilder();
        for (String line : sql.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--")) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        boolean escaped = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            current.append(ch);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '\'' && !doubleQuote && !backtick) {
                singleQuote = !singleQuote;
            } else if (ch == '"' && !singleQuote && !backtick) {
                doubleQuote = !doubleQuote;
            } else if (ch == '`' && !singleQuote && !doubleQuote) {
                backtick = !backtick;
            } else if (ch == ';' && !singleQuote && !doubleQuote && !backtick) {
                statements.add(current.toString());
                current.setLength(0);
            }
        }
        if (StringUtils.isNotBlank(current.toString())) {
            statements.add(current.toString());
        }
        return statements;
    }

    private User getAdminLoginUser(HttpServletRequest request) {
        User loginUser = SessionUserContext.getOptional(request);
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅管理员可访问");
        }
        return loginUser;
    }

    @Data
    public static class ImportResultVO {
        private Long batchId;
        private Integer created;
        private Integer skipped;
        private List<String> errors;
    }

    @Data
    public static class BackupStatusVO {
        private String backupDir;
        private Integer backupCount;
        private BackupFileVO latestBackup;
        private Boolean restoreEnabled;
    }

    @Data
    public static class BackupFileVO {
        private String filename;
        private Long size;
        private String sizeText;
        private String createTime;
    }

    @Data
    public static class RestoreBackupRequest {
        private String filename;
    }
}
