package com.ruyi.teach.service;

import com.ruyi.teach.model.vo.CodingRunResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalCodeExecutorTest {

    @Test
    void javaExecutorPassesStdinAndCapturesStdout() {
        LocalCodeExecutor executor = new LocalCodeExecutor(5000);
        String code = """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int n = scanner.nextInt();
                        int[] arr = new int[n];
                        for (int i = 0; i < n; i++) {
                            arr[i] = scanner.nextInt();
                        }
                        Arrays.sort(arr);
                        for (int i = 0; i < n; i++) {
                            if (i > 0) System.out.print(" ");
                            System.out.print(arr[i]);
                        }
                    }
                }
                """;

        CodingRunResultVO result = executor.execute("java", code, "5\n5 4 3 2 1\n", 5000L, 262144L);

        assertEquals("accepted", result.getStatus(), result.getStderr());
        assertEquals("1 2 3 4 5", result.getStdout());
    }
}
