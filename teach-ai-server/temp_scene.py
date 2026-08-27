from manim import *
from manim import *
import numpy as np

class GenScene(Scene):
    def construct(self):
        # 创建标题
        title = Text("勾股定理演示", font="Microsoft YaHei", color=WHITE)
        title.set_height(2)
        self.play(FadeIn(title))

        # 创建直角三角形
        A = np.array([0, 0, 0])
        B = np.array([3, 0, 0])
        C = np.array([0, 4, 0])
        triangle = Polygon(A, B, C, color=BLUE)
        self.play(FadeIn(triangle))

        # 创建斜边
        hypotenuse = Line(B, C, color=RED)
        self.play(FadeIn(hypotenuse))

        # 创建边长标签
        AB_label = Text("a", font="Microsoft YaHei", color=BLACK)
        BC_label = Text("b", font="Microsoft YaHei", color=BLACK)
        AC_label = Text("c", font="Microsoft YaHei", color=BLACK)
        AB_label.move_to(B + 0.5 * (C - B))
        BC_label.move_to(C + 0.5 * (A - C))
        AC_label.move_to(A + 0.5 * (B - A))
        self.play(FadeIn(AB_label), FadeIn(BC_label), FadeIn(AC_label))

        # 创建勾股定理公式
        pythagorean_theorem = Text("a² + b² = c²", font="Microsoft YaHei", color=BLACK)
        pythagorean_theorem.move_to(triangle.get_center() + np.array([0, -1, 0]))
        self.play(FadeIn(pythagorean_theorem))

        # 创建动画，使斜边长度变化
        self.play(hypotenuse.animate.set_length(5))
        self.play(hypotenuse.animate.set_length(4))
        self.play(hypotenuse.animate.set_length(3))

        # 创建动画，使边长标签变化
        self.play(AB_label.animate.set_height(1))
        self.play(BC_label.animate.set_height(1))
        self.play(AC_label.animate.set_height(1))

        # 创建动画，使勾股定理公式变化
        self.play(pythagorean_theorem.animate.set_height(1))

        # 结束动画
        self.wait()