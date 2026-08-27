package com.ruyi.teach.service.impl;

import com.ruyi.teach.model.entity.AiResource;
import com.ruyi.teach.model.entity.HomeworkAssignment;
import com.ruyi.teach.model.entity.HomeworkSubmissionDetail;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HomeworkSubmissionServiceImplAnswerParsingTest {

    @Test
    void typedAnswerSectionsDoNotLeakChoiceAnswersIntoFillQuestions() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单选题
                9. 下列说法正确的是（ ）。
                A. 选项一
                B. 选项二
                C. 选项三
                D. 选项四
                10. 下列输出结果正确的是（ ）。
                A. 选项一
                B. 选项二
                C. 选项三
                D. 选项四

                二、填空题
                9. 在C语言中，定义一个整型指针变量 p 并初始化为 NULL 的语句是：。
                10. 以下代码的输出结果是：。

                答案解析
                单选题答案解析
                9. 答案：C
                10. 答案：B

                填空题答案解析
                9. 答案：int *p = NULL;
                10. 答案：A
                """);

        List<?> metas = parseQuestionMetas(assignment);

        assertThat(field(findMeta(metas, "radio", "9"), "standardAnswer")).isEqualTo("C");
        assertThat(field(findMeta(metas, "radio", "10"), "standardAnswer")).isEqualTo("B");
        assertThat(field(findMeta(metas, "fill", "9"), "standardAnswer")).isEqualTo("int *p = NULL;");
        assertThat(field(findMeta(metas, "fill", "10"), "standardAnswer")).isEqualTo("A");
    }

    @Test
    void compactBracketedAnswerAnalysisStillProvidesChoiceAnswers() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                1. TCP三次握手中，客户端发送的第一个报文段中，SYN标志位和ACK标志位的值分别是多少？
                A. SYN=1, ACK=0
                B. SYN=1, ACK=1
                C. SYN=0, ACK=1
                D. SYN=0, ACK=0
                2. 在TCP三次握手的第二次握手过程中，服务器向客户端发送的报文段包含以下哪些标志位？
                A. 仅SYN
                B. 仅ACK
                C. SYN和ACK
                D. FIN和ACK
                3. 下列哪一项是TCP三次握手的核心目的？
                A. 确保数据传输的加密性
                B. 建立可靠连接并同步序列号
                C. 进行网络层路由选择
                D. 实现流量控制窗口协商
                ---
                答案解析区
                一、单项选择题解析
                1. 【答案】 A
                【解析】 第一次握手客户端发送SYN请求，ACK为0。 2. 【答案】 C
                【解析】 第二次握手服务器回复SYN和ACK。 3. 【答案】 B
                【解析】 三次握手用于建立可靠连接并同步双方初始序列号。
                """);

        List<?> metas = parseQuestionMetas(assignment);

        assertThat(field(findMeta(metas, "radio", "1"), "standardAnswer")).isEqualTo("A");
        assertThat(field(findMeta(metas, "radio", "2"), "standardAnswer")).isEqualTo("C");
        assertThat(field(findMeta(metas, "radio", "3"), "standardAnswer")).isEqualTo("B");
    }

    @Test
    void reportEnrichmentOverridesStaleImageFillStandardAnswer() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题
                1. 示例选择题？
                A. A项
                B. B项
                C. C项
                D. D项

                二、填空题
                9. 循环队列中，若数组大小为 m，则队列中最多能存储的元素个数为。
                10. 在循环队列中，若 front 指向队头元素，rear 指向队尾元素的下一个位置，则判断队列满的条件是（用 front、rear 和 m 表示）。

                ---
                参考答案与解析
                1. 答案：A
                9. 答案：m-1
                10. 答案：(rear + 1) % m == front
                """);

        List<HomeworkSubmissionDetail> details = new ArrayList<>();
        HomeworkSubmissionDetail q9 = new HomeworkSubmissionDetail();
        q9.setQuestionNo("9");
        q9.setQuestionType("fill");
        q9.setStemSnapshot("循环队列中，若数组大小为 m，则队列中最多能存储的元素个数为。");
        q9.setStudentAnswer("见图片作答");
        q9.setStandardAnswer("A");
        details.add(q9);

        HomeworkSubmissionDetail q10 = new HomeworkSubmissionDetail();
        q10.setQuestionNo("10");
        q10.setQuestionType("fill");
        q10.setStemSnapshot("在循环队列中，若 front 指向队头元素，rear 指向队尾元素的下一个位置，则判断队列满的条件是（用 front、rear 和 m 表示）。");
        q10.setStudentAnswer("见图片作答");
        q10.setStandardAnswer("A");
        details.add(q10);

        enrichReportDetails(details, assignment);

        assertThat(q9.getStandardAnswer()).isEqualTo("m-1");
        assertThat(q10.getStandardAnswer()).isEqualTo("(rear + 1) % m == front");
    }

    @Test
    void localQuestionNumberingUsesSequentialUntypedAnswerList() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题
                1. 选择题1？
                A. A项
                B. B项
                2. 选择题2？
                A. A项
                B. B项
                3. 选择题3？
                A. A项
                B. B项
                4. 选择题4？
                A. A项
                B. B项
                5. 选择题5？
                A. A项
                B. B项
                二、判断题
                1. 判断题1。（ ）
                2. 判断题2。（ ）
                3. 判断题3。（ ）
                三、填空题
                1. 循环队列中，若数组大小为 m，则队列中最多能存储的元素个数为。
                2. 在循环队列中，若 front 指向队头元素，rear 指向队尾元素的下一个位置，则判断队列满的条件是（用 front、rear 和 m 表示）。
                ---
                参考答案与解析
                1. 答案：A
                2. 答案：A
                3. 答案：A
                4. 答案：A
                5. 答案：A
                6. 答案：正确
                7. 答案：错误
                8. 答案：正确
                9. 答案：m-1
                10. 答案：(rear + 1) % m == front
                """);

        List<?> metas = parseQuestionMetas(assignment);

        assertThat(field(findMeta(metas, "fill", "1"), "standardAnswer")).isEqualTo("m-1");
        assertThat(field(findMeta(metas, "fill", "2"), "standardAnswer")).isEqualTo("(rear + 1) % m == front");
    }

    @Test
    void reportEnrichmentUsesGlobalQuestionNumberBeforeLocalTypeNumber() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题
                1. 单选1？
                A. A项
                B. B项
                2. 单选2？
                A. A项
                B. B项
                3. 单选3？
                A. A项
                B. B项
                4. 单选4？
                A. A项
                B. B项
                5. 单选5？
                A. A项
                B. B项

                二、判断题
                6. 判断1。（ ）
                7. 判断2。（ ）
                8. 判断3。（ ）
                9. 判断4。（ ）
                10. 判断5。（ ）

                三、填空题
                11. 队列允许插入的一端称为。
                12. 区分队空和队满，通常采用的方法是。
                13. 链队列通常设置的两个指针分别是。

                四、简答题
                14. 解释循环队列如何判断队空和队满。

                ---
                参考答案与解析
                1. 答案：A
                2. 答案：B
                3. 答案：A
                4. 答案：B
                5. 答案：A
                6. 答案：正确
                7. 答案：错误
                8. 答案：正确
                9. 答案：错误
                10. 答案：正确
                11. 答案：队尾
                12. 答案：牺牲一个存储单元
                13. 答案：front 和 rear
                14. 答案：队空为 front == rear，队满为 (rear + 1) % maxsize == front。
                """);

        HomeworkSubmissionDetail q12 = new HomeworkSubmissionDetail();
        q12.setQuestionNo("12");
        q12.setQuestionType("fill");
        q12.setStemSnapshot("旧题干快照");
        q12.setStandardAnswer("B");

        HomeworkSubmissionDetail q14 = new HomeworkSubmissionDetail();
        q14.setQuestionNo("14");
        q14.setQuestionType("text");
        q14.setStemSnapshot("旧简答题干快照");
        q14.setStandardAnswer("A");

        List<HomeworkSubmissionDetail> details = new ArrayList<>(List.of(q12, q14));
        enrichReportDetails(details, assignment);

        assertThat(q12.getQuestionType()).isEqualTo("fill");
        assertThat(q12.getStemSnapshot()).contains("区分队空和队满");
        assertThat(q12.getStandardAnswer()).isEqualTo("牺牲一个存储单元");
        assertThat(q14.getQuestionType()).isEqualTo("text");
        assertThat(q14.getStandardAnswer()).contains("front == rear");
    }

    @Test
    void inlineSeparatorDoesNotRemainInLastTextQuestionAndAnswersAreParsed() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题
                1. 单选1？
                A. A项
                B. B项

                二、简答题
                2. 什么是双端队列？请给出一个实际应用场景。 ---
                参考答案与解析
                1. 答案：B
                2. 答案：双端队列允许在队首和队尾两端进行插入和删除，常用于任务调度、缓存淘汰等场景。
                """);

        HomeworkSubmissionDetail q2 = new HomeworkSubmissionDetail();
        q2.setQuestionNo("2");
        q2.setQuestionType("text");
        q2.setStemSnapshot("什么是双端队列？请给出一个实际应用场景。 ---");
        q2.setStandardAnswer("B");

        enrichReportDetails(new ArrayList<>(List.of(q2)), assignment);

        assertThat(q2.getStemSnapshot()).doesNotContain("---");
        assertThat(q2.getStandardAnswer()).contains("双端队列允许");
    }

    @Test
    void groupedAnswerAnalysisWithRepeatedLocalNumbersUsesWholePaperOrder() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题（共5题）
                1、单选1？
                A. A项
                B. B项
                2、单选2？
                A. A项
                B. B项
                3、单选3？
                A. A项
                B. B项
                4、单选4？
                A. A项
                B. B项
                5、单选5？
                A. A项
                B. B项

                二、判断题（共5题）
                1、判断1。（ ）
                2、判断2。（ ）
                3、判断3。（ ）
                4、判断4。（ ）
                5、判断5。（ ）

                三、填空题（共5题）
                1、队列的英文名称是______。
                2、循环队列通常采用______个存储单元的方法。
                3、链队列通常设置______和______两个指针。
                4、队列应用包括______和______等。
                5、双端队列允许在______和______两端操作。

                四、简答题（共5题）
                1、简述队列与栈的主要区别。
                2、解释循环队列中如何判断队空和队满，并说明为什么要采用牺牲一个存储单元的方式。
                3、说明链队列的入队操作步骤，并分析其时间复杂度。
                4、举例说明队列在广度优先搜索（BFS）算法中的作用。
                5、什么是双端队列？请给出一个实际应用场景。

                ---
                ## 参考答案与解析
                一、单项选择题答案解析（共5题）
                1、答案：A
                2、答案：B
                3、答案：B
                4、答案：A
                5、答案：C
                二、判断题答案解析（共5题）
                1、答案：正确
                2、答案：错误
                3、答案：正确
                4、答案：正确
                5、答案：错误
                三、填空题答案解析（共5题）
                1、答案：Queue，先进先出
                2、答案：1
                3、答案：front，rear
                4、答案：进程调度，缓冲区管理
                5、答案：队头，队尾
                四、简答题答案解析（共5题）
                1、答案：队列先进先出，栈后进先出。
                2、答案：队空为 front == rear，队满为 (rear + 1) % MaxSize == front，牺牲一个单元可区分两种状态。
                3、答案：将新结点接到队尾并移动 rear 指针，时间复杂度 O(1)。
                4、答案：BFS 使用队列保存待访问结点，保证按层次遍历。
                5、答案：双端队列允许在队头和队尾插入删除，可用于任务调度。
                """);

        HomeworkSubmissionDetail q17 = new HomeworkSubmissionDetail();
        q17.setQuestionNo("17");
        q17.setQuestionType("text");
        q17.setStemSnapshot("解释循环队列中如何判断队空和队满，并说明为什么要采用牺牲一个存储单元的方式。");
        q17.setStandardAnswer("B");

        HomeworkSubmissionDetail q20 = new HomeworkSubmissionDetail();
        q20.setQuestionNo("20");
        q20.setQuestionType("text");
        q20.setStemSnapshot("什么是双端队列？请给出一个实际应用场景。");
        q20.setStandardAnswer("C");

        List<HomeworkSubmissionDetail> details = new ArrayList<>(List.of(q17, q20));
        enrichReportDetails(details, assignment);

        assertThat(q17.getStandardAnswer()).contains("front == rear");
        assertThat(q17.getStandardAnswer()).contains("牺牲一个单元");
        assertThat(q20.getStandardAnswer()).contains("双端队列允许");
        assertThat(q20.getStandardAnswer()).doesNotMatch("^[A-H]$");
    }

    @Test
    void answerHeadingsWithCountsAndMultilineTextAnswersKeepLaterSectionsAligned() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setContentSnapshot("""
                一、单项选择题（共5题）

                1、队列是一种特殊的线性表，其插入和删除操作分别发生在（ ）。
                A. 两端
                B. 同一端
                C. 仅在表头
                D. 仅在表尾

                2、循环队列中，判断队列满的条件是（假设队头指针为front，队尾指针为rear，队列最大容量为MaxSize）：
                A. front == rear
                B. (rear + 1) % MaxSize == front
                C. rear == MaxSize - 1
                D. front == 0

                二、判断题（共5题）

                1、队列的入队操作只能在队尾进行，出队操作只能在队头进行。（ ）
                2、循环队列中，当front==rear时，队列一定为空。（ ）

                三、填空题（共5题）

                1、队列的英文名称是______，其基本特征是______。
                2、在循环队列中，为了区分队空和队满，通常采用牺牲______个存储单元的方法。
                3、链队列中，通常设置______和______两个指针分别指向队头和队尾结点。

                四、简答题（共5题）

                1、简述队列与栈的主要区别。
                2、解释循环队列中如何判断队空和队满，并说明为什么要采用牺牲一个存储单元的方式。

                ---

                ## 参考答案与解析

                一、单项选择题答案解析（共5题）
                1、答案：A
                解析：队列是一种先进先出结构。
                2、答案：B
                解析：队满条件为 (rear + 1) % MaxSize == front。

                二、判断题答案解析（共5题）
                1、答案：正确
                解析：入队在队尾，出队在队头。
                2、答案：错误
                解析：front==rear 可能为空，也可能为满。

                三、填空题答案解析（共5题）
                1、答案：Queue；先进先出（FIFO）
                解析：队列的英文是 Queue。
                2、答案：1
                解析：牺牲一个存储单元。
                3、答案：front；rear
                解析：front 指向队头，rear 指向队尾。

                四、简答题答案解析（共5题）
                1、答案：
                栈是后进先出（LIFO）结构，队列是先进先出（FIFO）结构。
                - 栈的插入和删除都在栈顶进行。
                - 队列的插入在队尾，删除在队头。

                2、答案：
                队空条件是 front == rear。
                队满条件是 (rear + 1) % MaxSize == front。
                牺牲一个存储单元是为了区分队空和队满。
                """);

        HomeworkSubmissionDetail q11 = new HomeworkSubmissionDetail();
        q11.setQuestionNo("11");
        q11.setQuestionType("fill");
        q11.setStemSnapshot("队列的英文名称是，其基本特征是。");
        q11.setStandardAnswer("A");

        HomeworkSubmissionDetail q12 = new HomeworkSubmissionDetail();
        q12.setQuestionNo("12");
        q12.setQuestionType("fill");
        q12.setStemSnapshot("在循环队列中，为了区分队空和队满，通常采用牺牲个存储单元的方法。");
        q12.setStandardAnswer("B");

        HomeworkSubmissionDetail q16 = new HomeworkSubmissionDetail();
        q16.setQuestionNo("16");
        q16.setQuestionType("text");
        q16.setStemSnapshot("简述队列与栈的主要区别。");
        q16.setStandardAnswer("A");

        List<HomeworkSubmissionDetail> details = new ArrayList<>(List.of(q11, q12, q16));
        enrichReportDetails(details, assignment);

        assertThat(q11.getStandardAnswer()).isEqualTo("Queue；先进先出（FIFO）");
        assertThat(q12.getStandardAnswer()).isEqualTo("1");
        assertThat(q16.getStandardAnswer()).contains("栈是后进先出");
        assertThat(q16.getStandardAnswer()).contains("队列是先进先出");
        assertThat(q16.getStandardAnswer()).doesNotMatch("^[A-H]$");
    }

    @Test
    void reviewCommentPromptTreatsUploadedImagesAsSubmittedAnswers() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setTitle("循环队列练习");

        HomeworkSubmissionDetail detail = new HomeworkSubmissionDetail();
        detail.setId(99L);
        detail.setQuestionNo("9");
        detail.setQuestionType("fill");
        detail.setStemSnapshot("循环队列中，若数组大小为 m，则队列中最多能存储的元素个数为。");
        detail.setStudentAnswer("见图片作答");
        detail.setImageUrlsJson("[\"/uploads/q9.png\"]");
        detail.setStandardAnswer("m-1");
        detail.setFullScore(10);
        detail.setScore(0);

        String prompt = buildHomeworkReviewCommentPrompt(assignment, List.of(detail));

        assertThat(prompt).contains("学生已上传图片作答");
        assertThat(prompt).contains("不代表未提交");
        assertThat(prompt).contains("不能说未提交、未作答或需要补交文字答案");
    }

    @Test
    void studentHomeworkHistoryHidesDeletedMissingAndExamAssignments() throws Exception {
        HomeworkAssignment activeHomework = new HomeworkAssignment();
        activeHomework.setAssignmentType("homework");
        activeHomework.setIsDelete(0);

        HomeworkAssignment deletedHomework = new HomeworkAssignment();
        deletedHomework.setAssignmentType("homework");
        deletedHomework.setIsDelete(1);

        HomeworkAssignment exam = new HomeworkAssignment();
        exam.setAssignmentType("exam");
        exam.setIsDelete(0);

        HomeworkAssignment chapterPractice = new HomeworkAssignment();
        chapterPractice.setAssignmentType("chapter_practice");
        chapterPractice.setIsDelete(0);

        assertThat(isVisibleStudentHomeworkHistoryAssignment(activeHomework)).isTrue();
        assertThat(isVisibleStudentHomeworkHistoryAssignment(deletedHomework)).isFalse();
        assertThat(isVisibleStudentHomeworkHistoryAssignment(exam)).isFalse();
        assertThat(isVisibleStudentHomeworkHistoryAssignment(chapterPractice)).isFalse();
        assertThat(isVisibleStudentHomeworkHistoryAssignment(null)).isFalse();
    }

    @Test
    void studentHomeworkHistoryRequiresActiveQuizResource() throws Exception {
        HomeworkAssignment assignment = new HomeworkAssignment();
        assignment.setAssignmentType("homework");
        assignment.setIsDelete(0);
        assignment.setQuizResourceId(99L);

        AiResource activeQuiz = new AiResource();
        activeQuiz.setId(99L);
        activeQuiz.setType("quiz");
        activeQuiz.setIsDelete(0);

        AiResource deletedQuiz = new AiResource();
        deletedQuiz.setId(99L);
        deletedQuiz.setType("quiz");
        deletedQuiz.setIsDelete(1);

        AiResource plan = new AiResource();
        plan.setId(99L);
        plan.setType("plan");
        plan.setIsDelete(0);

        assertThat(isActiveAssignmentQuizResource(assignment, Map.of(99L, activeQuiz))).isTrue();
        assertThat(isActiveAssignmentQuizResource(assignment, Map.of(99L, deletedQuiz))).isFalse();
        assertThat(isActiveAssignmentQuizResource(assignment, Map.of(99L, plan))).isFalse();
        assertThat(isActiveAssignmentQuizResource(assignment, Map.of())).isFalse();
    }

    @SuppressWarnings("unchecked")
    private List<?> parseQuestionMetas(HomeworkAssignment assignment) throws Exception {
        HomeworkSubmissionServiceImpl service = new HomeworkSubmissionServiceImpl();
        Method method = HomeworkSubmissionServiceImpl.class.getDeclaredMethod("parseQuestionMetas", HomeworkAssignment.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(service, assignment);
    }

    private Object findMeta(List<?> metas, String type, String no) throws Exception {
        for (Object meta : metas) {
            if (type.equals(field(meta, "type")) && no.equals(field(meta, "no"))) {
                return meta;
            }
        }
        throw new AssertionError("Missing meta " + type + "#" + no);
    }

    private Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private void enrichReportDetails(List<HomeworkSubmissionDetail> details, HomeworkAssignment assignment) throws Exception {
        HomeworkSubmissionServiceImpl service = new HomeworkSubmissionServiceImpl();
        Method method = HomeworkSubmissionServiceImpl.class.getDeclaredMethod("enrichReportDetails", List.class, HomeworkAssignment.class);
        method.setAccessible(true);
        method.invoke(service, details, assignment);
    }

    private boolean isVisibleStudentHomeworkHistoryAssignment(HomeworkAssignment assignment) throws Exception {
        HomeworkSubmissionServiceImpl service = new HomeworkSubmissionServiceImpl();
        Method method = HomeworkSubmissionServiceImpl.class.getDeclaredMethod(
                "isVisibleStudentHomeworkHistoryAssignment",
                HomeworkAssignment.class
        );
        method.setAccessible(true);
        return (Boolean) method.invoke(service, assignment);
    }

    private boolean isActiveAssignmentQuizResource(HomeworkAssignment assignment,
                                                   Map<Long, AiResource> quizResourceMap) throws Exception {
        HomeworkSubmissionServiceImpl service = new HomeworkSubmissionServiceImpl();
        Method method = HomeworkSubmissionServiceImpl.class.getDeclaredMethod(
                "isActiveAssignmentQuizResource",
                HomeworkAssignment.class,
                Map.class
        );
        method.setAccessible(true);
        return (Boolean) method.invoke(service, assignment, quizResourceMap);
    }

    private String buildHomeworkReviewCommentPrompt(HomeworkAssignment assignment,
                                                    List<HomeworkSubmissionDetail> details) throws Exception {
        HomeworkSubmissionServiceImpl service = new HomeworkSubmissionServiceImpl();
        Method method = HomeworkSubmissionServiceImpl.class.getDeclaredMethod("buildHomeworkReviewCommentPrompt", HomeworkAssignment.class, List.class, java.util.Map.class);
        method.setAccessible(true);
        return (String) method.invoke(service, assignment, details, java.util.Map.of());
    }
}
