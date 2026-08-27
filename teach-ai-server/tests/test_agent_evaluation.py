import copy
import unittest

from evaluation.runner import (
    DEFAULT_BASELINE_PATH,
    compare_with_baseline,
    evaluate_suite,
    load_json,
    load_suite,
)


class FixedAgentEvaluationTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.suite, cls.corpus = load_suite()
        cls.report = evaluate_suite(cls.suite, cls.corpus)
        cls.baseline = load_json(DEFAULT_BASELINE_PATH)

    def test_suite_has_twenty_versioned_unique_tasks(self):
        tasks = self.suite["tasks"]

        self.assertEqual(20, len(tasks))
        self.assertEqual(20, len({task["id"] for task in tasks}))
        self.assertEqual("offline_deterministic", self.suite["executionMode"])
        self.assertTrue(self.suite["promptContractVersion"])
        self.assertTrue(self.suite["model"]["offlineAdapterVersion"])

    def test_fixed_suite_meets_baseline_without_isolation_leaks(self):
        self.assertEqual([], compare_with_baseline(self.report, self.baseline))
        self.assertEqual(0, self.report["aggregate"]["failed_task_count"])
        self.assertEqual(
            0,
            self.report["aggregate"]["isolation_violation_count"],
        )
        self.assertTrue(all(task["passed"] for task in self.report["tasks"]))

    def test_baseline_comparison_detects_retrieval_regression(self):
        regressed = copy.deepcopy(self.report)
        regressed["aggregate"]["retrieval_hit_rate"] = 0.95
        regressed["aggregate"]["failed_task_count"] = 1

        failures = compare_with_baseline(regressed, self.baseline)

        self.assertTrue(
            any("retrieval_hit_rate" in failure for failure in failures)
        )
        self.assertIn("failed task count exceeds baseline", failures)


if __name__ == "__main__":
    unittest.main()
