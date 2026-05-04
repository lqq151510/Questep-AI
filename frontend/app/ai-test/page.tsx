"use client";

import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Clock3, ListChecks, Play, RefreshCw, Sparkles } from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import { useToast } from "@/components/new-ui/ToastProvider";
import {
  generateQuiz,
  listMaterials,
  listQuestions,
  toErrorMessage,
  type BackendMaterial,
  type BackendQuestion,
  type GenerateQuizPayload
} from "@/lib/interview-api";

type Question = {
  id: number;
  stem: string;
  type: "single";
  tags: string[];
  options: string[];
  answer: number[];
  reference: string;
};

const tracks = ["Java", "Spring", "MySQL", "Redis", "系统设计"] as const;
const WRONG_BOOK_KEY = "wrong_question_records";

type WrongRecord = {
  id: string;
  title: string;
  category: string;
  level: "高频" | "常规";
  mastered: boolean;
};

const TRACK_KEYWORDS: Record<(typeof tracks)[number], string[]> = {
  Java: ["java", "jvm", "并发", "线程"],
  Spring: ["spring", "事务", "aop", "ioc"],
  MySQL: ["mysql", "索引", "sql", "数据库"],
  Redis: ["redis", "缓存", "持久化"],
  系统设计: ["系统设计", "架构", "高并发", "可用性"]
};

function formatSeconds(totalSeconds: number) {
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function inferTags(stem: string): string[] {
  const lower = stem.toLowerCase();
  const matched = tracks.filter((track) => TRACK_KEYWORDS[track].some((keyword) => lower.includes(keyword.toLowerCase())));
  return matched.length > 0 ? [...matched] : ["Java"];
}

function selectQuestionType(selectedTracks: string[]): GenerateQuizPayload["questionType"] {
  if (selectedTracks.includes("系统设计")) return "interview";
  if (selectedTracks.includes("MySQL") || selectedTracks.includes("Redis")) return "choice";
  if (selectedTracks.includes("Java") || selectedTracks.includes("Spring")) return "short";
  return "short";
}

function fallbackOptions(reference: string): string[] {
  return [
    reference || "参考答案待补充",
    "只需要记住定义，不用考虑边界场景",
    "主要依赖框架默认行为，无需额外设计",
    "该问题没有通用方法，无法系统化回答"
  ];
}

function toWrongRecords(
  questionList: Question[],
  answerMap: Record<number, number[]>,
  difficulty: number
): WrongRecord[] {
  return questionList
    .filter((question) => {
      const selected = [...(answerMap[question.id] ?? [])].sort((a, b) => a - b);
      const answer = [...question.answer].sort((a, b) => a - b);
      return selected.length !== answer.length || !selected.every((value, idx) => value === answer[idx]);
    })
    .map((question) => ({
      id: `w-${question.id}`,
      title: question.stem,
      category: question.tags[0] ?? "通用",
      level: difficulty >= 4 ? "高频" : "常规",
      mastered: false
    }));
}

function mergeWrongRecords(next: WrongRecord[]): WrongRecord[] {
  if (typeof window === "undefined") return next;
  const raw = window.localStorage.getItem(WRONG_BOOK_KEY);
  if (!raw) return next;
  try {
    const existing = JSON.parse(raw) as WrongRecord[];
    const merged = new Map<string, WrongRecord>();
    for (const item of existing) {
      merged.set(item.id, item);
    }
    for (const item of next) {
      merged.set(item.id, item);
    }
    return Array.from(merged.values());
  } catch {
    return next;
  }
}

function toQuestion(item: BackendQuestion): Question {
  const reference = item.referenceAnswer?.trim() || item.analysisText?.trim() || "建议结合项目经验分点回答。";
  return {
    id: item.id,
    stem: item.stemText,
    type: "single",
    tags: inferTags(item.stemText),
    options: fallbackOptions(reference),
    answer: [0],
    reference
  };
}

function filterMaterialsByTracks(materials: BackendMaterial[], selectedTracks: string[]): number[] {
  if (selectedTracks.length === 0) return materials.map((item) => item.id);
  const selected = materials.filter((material) => {
    const text = `${material.name} ${material.fileType}`.toLowerCase();
    return selectedTracks.some((track) => TRACK_KEYWORDS[track as (typeof tracks)[number]].some((keyword) => text.includes(keyword.toLowerCase())));
  });
  return (selected.length > 0 ? selected : materials).map((item) => item.id);
}

export default function AiTestPage() {
  const { showToast } = useToast();
  const [selectedTracks, setSelectedTracks] = useState<string[]>(["Java", "Spring"]);
  const [difficulty, setDifficulty] = useState(3);
  const [started, setStarted] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number[]>>({});
  const [questions, setQuestions] = useState<Question[]>([]);
  const [materials, setMaterials] = useState<BackendMaterial[]>([]);
  const [loadingMaterials, setLoadingMaterials] = useState(false);
  const [generating, setGenerating] = useState(false);

  const refreshMaterials = async () => {
    setLoadingMaterials(true);
    try {
      const data = await listMaterials();
      const readyMaterials = data.filter((item) => String(item.parseStatus ?? "").toUpperCase() === "SUCCESS");
      setMaterials(readyMaterials);
    } catch (error) {
      showToast(toErrorMessage(error, "加载资料失败"));
    } finally {
      setLoadingMaterials(false);
    }
  };

  useEffect(() => {
    void refreshMaterials();
  }, []);

  useEffect(() => {
    if (!started || submitted) {
      return;
    }
    const timer = window.setInterval(() => setElapsed((prev) => prev + 1), 1000);
    return () => window.clearInterval(timer);
  }, [started, submitted]);

  const currentQuestion = questions[currentIndex];
  const progress = questions.length > 0 ? ((currentIndex + 1) / questions.length) * 100 : 0;

  const score = useMemo(() => {
    let correct = 0;
    for (const question of questions) {
      const selected = [...(answers[question.id] ?? [])].sort((a, b) => a - b);
      const answer = [...question.answer].sort((a, b) => a - b);
      if (selected.length === answer.length && selected.every((value, idx) => value === answer[idx])) {
        correct += 1;
      }
    }
    return correct;
  }, [answers, questions]);

  const accuracy = questions.length ? Math.round((score / questions.length) * 100) : 0;

  const toggleTrack = (track: string) => {
    setSelectedTracks((prev) =>
      prev.includes(track) ? prev.filter((item) => item !== track) : [...prev, track]
    );
  };

  const loadQuestions = async () => {
    const materialIds = filterMaterialsByTracks(materials, selectedTracks);
    if (materialIds.length === 0) {
      throw new Error("没有可用资料，请先在知识库上传并解析资料。");
    }

    const result = await generateQuiz({
      materialIds,
      questionType: selectQuestionType(selectedTracks),
      difficulty,
      count: 5,
      interviewMode: true
    });
    const fromGeneration = (result.questions ?? []).map(toQuestion);
    if (fromGeneration.length > 0) {
      return fromGeneration;
    }

    const recent = await listQuestions(10);
    const fallback = recent.map(toQuestion);
    if (fallback.length === 0) {
      throw new Error("暂无可用题目，请先生成题目。");
    }
    return fallback;
  };

  const startQuiz = async () => {
    if (selectedTracks.length === 0) {
      showToast("请至少选择一个训练方向");
      return;
    }

    setGenerating(true);
    try {
      const loaded = await loadQuestions();
      setQuestions(loaded);
      setStarted(true);
      setSubmitted(false);
      setElapsed(0);
      setCurrentIndex(0);
      setAnswers({});
      showToast(`测试已开始，本轮共 ${loaded.length} 题`);
    } catch (error) {
      showToast(toErrorMessage(error, "组卷失败"));
    } finally {
      setGenerating(false);
    }
  };

  const selectOption = (optionIndex: number) => {
    if (submitted || !currentQuestion) {
      return;
    }
    const key = currentQuestion.id;
    setAnswers((prev) => ({ ...prev, [key]: [optionIndex] }));
  };

  const submitQuiz = () => {
    setSubmitted(true);
    const wrongRecords = toWrongRecords(questions, answers, difficulty);
    const merged = mergeWrongRecords(wrongRecords);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(WRONG_BOOK_KEY, JSON.stringify(merged));
    }
    showToast("测试已提交，已生成结果");
  };

  return (
    <div className="container">
      <PageHero
        kicker="Adaptive Quiz"
        title="AI 专项测试"
        description="组合岗位方向、难度和题型，实时组卷并跟踪答题质量。"
      />

      {!started ? (
        <section className="panel form-panel">
          <h2>测试配置</h2>
          <div className="field-group">
            <p className="field-label">训练方向</p>
            <div className="chip-row">
              {tracks.map((track) => (
                <button
                  key={track}
                  type="button"
                  className={selectedTracks.includes(track) ? "chip active" : "chip"}
                  onClick={() => toggleTrack(track)}
                >
                  {track}
                </button>
              ))}
            </div>
          </div>

          <div className="field-group">
            <label htmlFor="difficulty" className="field-label">
              难度系数
            </label>
            <div className="slider-row">
              <input
                id="difficulty"
                type="range"
                min={1}
                max={5}
                value={difficulty}
                onChange={(event) => setDifficulty(Number(event.target.value))}
              />
              <span className="slider-value">{difficulty}</span>
            </div>
          </div>

          <div className="row-actions">
            <button type="button" className="btn" onClick={() => void refreshMaterials()} disabled={loadingMaterials}>
              <RefreshCw size={14} />
              {loadingMaterials ? "刷新中" : `可用资料 ${materials.length} 份`}
            </button>
            <button type="button" className="btn btn-accent wide" onClick={() => void startQuiz()} disabled={generating}>
              <Play size={16} />
              {generating ? "组卷中..." : "开始测试"}
            </button>
          </div>
        </section>
      ) : (
        <section className="panel quiz-panel">
          <div className="quiz-head">
            <p>
              <Clock3 size={14} /> {formatSeconds(elapsed)}
            </p>
            <p>
              <ListChecks size={14} /> 第 {currentIndex + 1} / {questions.length} 题
            </p>
          </div>

          <div className="progress-track">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>

          {!submitted && currentQuestion ? (
            <>
              <article className="question-card">
                <p className="question-type">
                  单选题 · 难度 {difficulty}
                </p>
                <h3>{currentQuestion.stem}</h3>
                <div className="option-list">
                  {currentQuestion.options.map((option, index) => {
                    const checked = (answers[currentQuestion.id] ?? []).includes(index);
                    return (
                      <button
                        key={`${currentQuestion.id}-${index}`}
                        type="button"
                        className={checked ? "option-card active" : "option-card"}
                        onClick={() => selectOption(index)}
                      >
                        <span className="option-index">{String.fromCharCode(65 + index)}</span>
                        <span>{option}</span>
                      </button>
                    );
                  })}
                </div>
              </article>

              <div className="row-actions">
                <button
                  type="button"
                  className="btn"
                  disabled={currentIndex === 0}
                  onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
                >
                  上一题
                </button>
                {currentIndex < questions.length - 1 ? (
                  <button
                    type="button"
                    className="btn btn-accent"
                    onClick={() => setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))}
                  >
                    下一题
                  </button>
                ) : (
                  <button type="button" className="btn btn-accent" onClick={submitQuiz}>
                    提交测试
                  </button>
                )}
              </div>
            </>
          ) : (
            <article className="result-panel">
              <p className="result-badge">
                <CheckCircle2 size={16} /> 已完成
              </p>
              <h3>本轮正确率 {accuracy}%</h3>
              <p>
                共答对 {score} / {questions.length} 题，建议针对错误题目进入错题本进行二次强化。
              </p>
              <div className="row-actions">
                <button type="button" className="btn btn-accent" onClick={() => void startQuiz()}>
                  再测一轮
                </button>
                <button type="button" className="btn" onClick={() => showToast("已推送到错题追踪")}>
                  <Sparkles size={14} />
                  加入错题追踪
                </button>
              </div>
            </article>
          )}
        </section>
      )}
    </div>
  );
}
