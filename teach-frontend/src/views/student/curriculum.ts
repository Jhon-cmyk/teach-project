/**
 * 计算机科学与技术专业 四年制培养方案课程库
 * 说明：此数据在正式版本中应从管理端配置，当前为演示用静态数据
 */

export interface CurriculumCourse {
  name: string
  credits: number
  category: string
  semester: number
  type: string
  prerequisites: string[]
  description: string
}

export const ALL_COURSES: CurriculumCourse[] = [
  // ===== 第1学期 =====
  { name: '高等数学(一)', credits: 5, category: '基础课', semester: 1, type: '必修', prerequisites: [], description: '极限、导数、微分、不定积分与定积分，是后续所有理工科课程的数学基础。' },
  { name: '大学英语(一)', credits: 3, category: '通识课', semester: 1, type: '必修', prerequisites: [], description: '培养学生英语听说读写综合能力，为专业英语打基础。' },
  { name: '思想政治理论课I', credits: 3, category: '通识课', semester: 1, type: '必修', prerequisites: [], description: '马克思主义基本原理概论，培养正确的世界观和方法论。' },
  { name: 'C程序设计', credits: 4, category: '专业核心', semester: 1, type: '必修', prerequisites: [], description: '学习C语言语法、指针、数组、结构体、文件操作等，是计算机专业的第一门编程语言。' },
  { name: '计算机导论', credits: 2, category: '基础课', semester: 1, type: '必修', prerequisites: [], description: '介绍计算机科学的基本概念、发展历史、应用领域，建立专业全景认知。' },
  { name: '体育(一)', credits: 1, category: '通识课', semester: 1, type: '必修', prerequisites: [], description: '增强体质，培养终身体育意识。' },

  // ===== 第2学期 =====
  { name: '高等数学(二)', credits: 5, category: '基础课', semester: 2, type: '必修', prerequisites: ['高等数学(一)'], description: '多元函数微积分、无穷级数、常微分方程，为概率论和信号处理提供工具。' },
  { name: '线性代数', credits: 3, category: '基础课', semester: 2, type: '必修', prerequisites: ['高等数学(一)'], description: '矩阵运算、向量空间、特征值，在机器学习和图形学中广泛应用。' },
  { name: '离散数学', credits: 4, category: '基础课', semester: 2, type: '必修', prerequisites: [], description: '命题逻辑、集合论、图论与代数结构，是算法和编译原理的数学基础。' },
  { name: '大学英语(二)', credits: 3, category: '通识课', semester: 2, type: '必修', prerequisites: ['大学英语(一)'], description: '进一步提升英语综合运用能力，增强学术英语读写。' },
  { name: '大学物理', credits: 4, category: '基础课', semester: 2, type: '必修', prerequisites: ['高等数学(一)'], description: '力学、热学、电磁学、光学基础，理解硬件工作的物理原理。' },
  { name: '体育(二)', credits: 1, category: '通识课', semester: 2, type: '必修', prerequisites: [], description: '专项体育技能学习与体能训练。' },

  // ===== 第3学期 =====
  { name: '数据结构', credits: 4, category: '专业核心', semester: 3, type: '必修', prerequisites: ['C程序设计', '离散数学'], description: '线性表、栈、队列、树、图、散列表等数据组织方式及基本算法，是所有高级专业课的基石。' },
  { name: '概率论与数理统计', credits: 3, category: '基础课', semester: 3, type: '必修', prerequisites: ['高等数学(二)'], description: '随机事件与概率、随机变量、数理统计基础，为数据分析和机器学习提供理论支撑。' },
  { name: '数字逻辑电路', credits: 3, category: '专业核心', semester: 3, type: '必修', prerequisites: ['大学物理'], description: '布尔代数、组合逻辑、时序逻辑电路设计，理解计算机硬件的底层实现。' },
  { name: '面向对象程序设计(Java)', credits: 3, category: '专业核心', semester: 3, type: '必修', prerequisites: ['C程序设计'], description: '面向对象思想、封装继承多态、异常处理、集合框架，Java是企业级开发的主流语言。' },
  { name: '思想政治理论课II', credits: 3, category: '通识课', semester: 3, type: '必修', prerequisites: [], description: '中国近现代史纲要，了解国家发展历程。' },

  // ===== 第4学期 =====
  { name: '计算机组成原理', credits: 4, category: '专业核心', semester: 4, type: '必修', prerequisites: ['数字逻辑电路'], description: '运算器、控制器、存储器、总线与I/O系统的工作原理，连接软件与硬件的桥梁。' },
  { name: '操作系统', credits: 4, category: '专业核心', semester: 4, type: '必修', prerequisites: ['数据结构', 'C程序设计'], description: '进程管理、内存管理、文件系统、设备管理，理解程序如何在硬件上被调度执行。' },
  { name: '数据库原理及应用', credits: 3, category: '专业核心', semester: 4, type: '必修', prerequisites: ['数据结构'], description: '关系模型、SQL语言、事务处理、数据库设计范式，是所有信息系统的数据基础。' },
  { name: '算法设计与分析', credits: 3, category: '专业核心', semester: 4, type: '必修', prerequisites: ['数据结构', '离散数学'], description: '分治、动态规划、贪心、回溯等算法策略，培养计算思维和问题求解能力。' },
  { name: 'Web前端开发', credits: 2, category: '专业选修', semester: 4, type: '选修', prerequisites: ['面向对象程序设计(Java)'], description: 'HTML/CSS/JavaScript基础，Vue.js框架入门，构建用户界面。' },

  // ===== 第5学期 =====
  { name: '计算机网络', credits: 4, category: '专业核心', semester: 5, type: '必修', prerequisites: ['操作系统'], description: 'TCP/IP五层模型、HTTP协议、路由算法、网络安全基础，理解互联网工作原理。' },
  { name: '编译原理', credits: 3, category: '专业核心', semester: 5, type: '必修', prerequisites: ['数据结构', '离散数学'], description: '词法分析、语法分析、语义分析与代码生成，理解编程语言如何被翻译成机器码。' },
  { name: '软件工程', credits: 3, category: '专业核心', semester: 5, type: '必修', prerequisites: ['数据库原理及应用', '面向对象程序设计(Java)'], description: '需求分析、系统设计、测试与维护的方法论，UML建模、敏捷开发流程。' },
  { name: '单片机原理及应用', credits: 3, category: '专业选修', semester: 5, type: '选修', prerequisites: ['计算机组成原理', '数字逻辑电路'], description: '51/STM32单片机编程，中断、定时器、串口通信，嵌入式系统入门。' },
  { name: '计算机专业英语', credits: 2, category: '通识课', semester: 5, type: '必修', prerequisites: ['大学英语(二)'], description: '阅读英文技术文档、论文摘要写作、IT行业常用术语。' },

  // ===== 第6学期 =====
  { name: '网络安全技术', credits: 3, category: '专业选修', semester: 6, type: '选修', prerequisites: ['计算机网络'], description: '加密算法、防火墙、入侵检测、Web安全漏洞与防护。' },
  { name: '路由与交换技术', credits: 3, category: '专业选修', semester: 6, type: '选修', prerequisites: ['计算机网络'], description: 'VLAN配置、OSPF/BGP路由协议、ACL访问控制，Cisco/华为设备实操。' },
  { name: '人工智能导论', credits: 3, category: '专业选修', semester: 6, type: '选修', prerequisites: ['概率论与数理统计', '算法设计与分析'], description: '搜索算法、知识表示、机器学习基础、神经网络入门。' },
  { name: 'Python程序设计', credits: 2, category: '专业选修', semester: 6, type: '选修', prerequisites: ['C程序设计'], description: 'Python语法、数据处理、爬虫基础、自动化脚本编写。' },
  { name: '数据库课程设计', credits: 2, category: '实践环节', semester: 6, type: '必修', prerequisites: ['数据库原理及应用'], description: '综合运用数据库知识完成实际项目，包括需求分析、E-R建模、SQL实现。' },
  { name: '大学生安全教育', credits: 1, category: '通识课', semester: 6, type: '必修', prerequisites: [], description: '国家安全、网络安全、人身安全等方面的教育。' },

  // ===== 第7学期 =====
  { name: '机器学习', credits: 3, category: '专业选修', semester: 7, type: '选修', prerequisites: ['人工智能导论', '线性代数', '概率论与数理统计'], description: '线性回归、SVM、决策树、集成学习、聚类分析，用sklearn实现经典算法。' },
  { name: '大数据技术', credits: 3, category: '专业选修', semester: 7, type: '选修', prerequisites: ['数据库原理及应用', 'Python程序设计'], description: 'Hadoop/Spark生态、分布式存储与计算、数据仓库与ETL流程。' },
  { name: '软件项目管理', credits: 2, category: '专业选修', semester: 7, type: '选修', prerequisites: ['软件工程'], description: '项目计划、进度控制、风险管理、团队协作，Scrum/Kanban实践。' },
  { name: '计算机网络编程', credits: 2, category: '专业选修', semester: 7, type: '选修', prerequisites: ['计算机网络', '面向对象程序设计(Java)'], description: 'Socket编程、HTTP服务器实现、WebSocket实时通信。' },
  { name: '大学生职业规划与就业指导', credits: 1, category: '通识课', semester: 7, type: '必修', prerequisites: [], description: '简历撰写、面试技巧、职业发展规划。' },
  { name: '网络编程综合实践', credits: 2, category: '实践环节', semester: 7, type: '必修', prerequisites: ['计算机网络编程'], description: '综合网络编程项目实践，完成一个网络应用系统。' },

  // ===== 第8学期 =====
  { name: '毕业设计', credits: 12, category: '实践环节', semester: 8, type: '必修', prerequisites: [], description: '综合运用四年所学知识，独立完成一个完整项目，撰写毕业论文并答辩。' },
  { name: '专业见习', credits: 2, category: '实践环节', semester: 8, type: '必修', prerequisites: [], description: '到企业实地参观学习，了解行业实际工作流程和岗位需求。' },
  { name: '大学美育', credits: 1, category: '通识课', semester: 8, type: '选修', prerequisites: [], description: '培养审美能力和人文素养，提升综合素质。' },
]

export const getAllCategories = (): string[] => {
  return [...new Set(ALL_COURSES.map(c => c.category))]
}
