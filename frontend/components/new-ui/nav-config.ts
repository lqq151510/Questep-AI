export type NavItem = {
  href: string;
  label: string;
};

export const NAV_ITEMS: NavItem[] = [
  { href: "/", label: "首页" },
  { href: "/home", label: "总览" },
  { href: "/ai-test", label: "AI 测试" },
  { href: "/ai-interviewer", label: "AI 面试官" },
  { href: "/ai-qa", label: "AI 问答" },
  { href: "/knowledge-base", label: "知识库" },
  { href: "/question-bank", label: "题库" },
  { href: "/wrong-answers", label: "错题本" },
  { href: "/interview-tips", label: "面试技巧" }
];
