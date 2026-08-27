package com.ruyi.teach.service;

import com.ruyi.teach.model.vo.CodingRunResultVO;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class LocalCodeExecutor implements CodeExecutor {

    private final long defaultTimeoutMs;
    private final Path tempDir;

    public LocalCodeExecutor(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.tempDir = PathsToTempDir();
    }

    private static Path PathsToTempDir() {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "coding-sandbox");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建沙箱临时目录: " + dir, e);
        }
        return dir;
    }

    @Override
    public CodingRunResultVO execute(String language, String code, String stdin,
                                     Long timeoutMs, Long memoryLimitKb) {
        long timeout = timeoutMs != null ? timeoutMs : defaultTimeoutMs;
        return switch (language.toLowerCase()) {
            case "java" -> executeJava(code, stdin, timeout);
            case "python" -> executePython(code, stdin, timeout);
            case "javascript" -> executeJavaScript(code, stdin, timeout);
            case "cpp" -> executeCpp(code, stdin, timeout);
            default -> CodingRunResultVO.error(language, "不支持的语言: " + language);
        };
    }

    private CodingRunResultVO executeJava(String code, String stdin, long timeoutMs) {
        String className = extractJavaClassName(code);
        if (className == null) {
            return CodingRunResultVO.error("java", "无法从代码中提取类名，请确保代码包含 public class 声明");
        }

        String javacPath = resolveJavaTool("javac");
        String javaPath = resolveJavaTool("java");

        Path workDir;
        try {
            workDir = Files.createTempDirectory(tempDir, "java_");
        } catch (IOException e) {
            return CodingRunResultVO.error("java", "创建临时目录失败: " + e.getMessage());
        }

        try {
            Path sourceFile = workDir.resolve(className + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            ProcessResult compileResult = runProcess(workDir, timeoutMs, null, javacPath, "-encoding", "UTF-8", sourceFile.getFileName().toString());
            if (compileResult.exitCode != 0) {
                return CodingRunResultVO.builder()
                        .language("java").status("compile_error")
                        .stderr(compileResult.stderr).build();
            }

            ProcessResult runResult = runProcess(workDir, timeoutMs, stdin, javaPath, "-Dfile.encoding=UTF-8", className);
            return buildResult("java", runResult);
        } catch (Exception e) {
            return CodingRunResultVO.error("java", e.getMessage());
        } finally {
            deleteDir(workDir);
        }
    }

    private CodingRunResultVO executePython(String code, String stdin, long timeoutMs) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory(tempDir, "py_");
        } catch (IOException e) {
            return CodingRunResultVO.error("python", "创建临时目录失败: " + e.getMessage());
        }

        try {
            Path sourceFile = workDir.resolve("solution.py");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            ProcessResult runResult = runProcess(workDir, timeoutMs, stdin, "python", sourceFile.toAbsolutePath().toString());
            return buildResult("python", runResult);
        } catch (Exception e) {
            return CodingRunResultVO.error("python", e.getMessage());
        } finally {
            deleteDir(workDir);
        }
    }

    private CodingRunResultVO executeJavaScript(String code, String stdin, long timeoutMs) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory(tempDir, "js_");
        } catch (IOException e) {
            return CodingRunResultVO.error("javascript", "创建临时目录失败: " + e.getMessage());
        }

        try {
            Path sourceFile = workDir.resolve("solution.js");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            ProcessResult runResult = runProcess(workDir, timeoutMs, stdin, "node", sourceFile.toAbsolutePath().toString());
            return buildResult("javascript", runResult);
        } catch (Exception e) {
            return CodingRunResultVO.error("javascript", e.getMessage());
        } finally {
            deleteDir(workDir);
        }
    }

    private CodingRunResultVO executeCpp(String code, String stdin, long timeoutMs) {
        Path workDir;
        try {
            workDir = Files.createTempDirectory(tempDir, "cpp_");
        } catch (IOException e) {
            return CodingRunResultVO.error("cpp", "创建临时目录失败: " + e.getMessage());
        }

        try {
            Path sourceFile = workDir.resolve("solution.cpp");
            Path outputFile = workDir.resolve("solution.exe");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            ProcessResult compileResult = runProcess(workDir, timeoutMs, null, "g++", "-o", outputFile.getFileName().toString(), sourceFile.getFileName().toString());
            if (compileResult.exitCode != 0) {
                return CodingRunResultVO.builder()
                        .language("cpp").status("compile_error")
                        .stderr(compileResult.stderr).build();
            }

            ProcessResult runResult = runProcess(workDir, timeoutMs, stdin, outputFile.toAbsolutePath().toString());
            return buildResult("cpp", runResult);
        } catch (Exception e) {
            return CodingRunResultVO.error("cpp", e.getMessage());
        } finally {
            deleteDir(workDir);
        }
    }

    private ProcessResult runProcess(Path workDir, long timeoutMs, String stdin, String... command) {
        ProcessResult result = new ProcessResult();
        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workDir.toFile())
                    .redirectErrorStream(false);

            Process process = pb.start();

            if (stdin != null && !stdin.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                process.getOutputStream().close();
            }

            ExecutorService executor = Executors.newFixedThreadPool(2);
            java.nio.charset.Charset processCharset = System.getProperty("os.name").toLowerCase().contains("win")
                    ? java.nio.charset.Charset.forName("GBK")
                    : StandardCharsets.UTF_8;
            Future<String> stdoutFuture = executor.submit(new StreamReader(process.getInputStream(), processCharset));
            Future<String> stderrFuture = executor.submit(new StreamReader(process.getErrorStream(), processCharset));

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            executor.shutdown();

            if (!finished) {
                process.destroyForcibly();
                result.exitCode = -1;
                result.stdout = "";
                result.stderr = "执行超时（超过 " + timeoutMs + "ms）";
                result.timedOut = true;
            } else {
                result.exitCode = process.exitValue();
                result.stdout = stdoutFuture.get(5, TimeUnit.SECONDS).trim();
                result.stderr = stderrFuture.get(5, TimeUnit.SECONDS).trim();
            }
        } catch (TimeoutException e) {
            result.exitCode = -1;
            result.stderr = "读取输出超时";
        } catch (Exception e) {
            result.exitCode = -1;
            result.stderr = "执行异常: " + e.getMessage();
        }
        return result;
    }

    private CodingRunResultVO buildResult(String language, ProcessResult pr) {
        String status = pr.exitCode == 0 ? "accepted" : "runtime_error";
        if (pr.timedOut) status = "time_limit_exceeded";

        return CodingRunResultVO.builder()
                .language(language)
                .status(status)
                .stdout(pr.stdout)
                .stderr(pr.stderr)
                .exitCode(pr.exitCode)
                .build();
    }

    private String extractJavaClassName(String code) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }

    private String resolveJavaTool(String toolName) {
        String executable = System.getProperty("os.name").toLowerCase().contains("win")
                ? toolName + ".exe"
                : toolName;

        Path runtimeTool = Path.of(System.getProperty("java.home"), "bin", executable);
        if (Files.isRegularFile(runtimeTool)) {
            return runtimeTool.toString();
        }

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            Path javaHomeTool = Path.of(javaHome, "bin", executable);
            if (Files.isRegularFile(javaHomeTool)) {
                return javaHomeTool.toString();
            }
        }

        return toolName;
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}
    }

    private static class ProcessResult {
        int exitCode = -1;
        String stdout = "";
        String stderr = "";
        boolean timedOut = false;
    }

    private static class StreamReader implements Callable<String> {
        private final InputStream is;
        private final java.nio.charset.Charset charset;
        StreamReader(InputStream is, java.nio.charset.Charset charset) { this.is = is; this.charset = charset; }
        @Override
        public String call() throws IOException {
            return new String(is.readAllBytes(), charset);
        }
    }
}
