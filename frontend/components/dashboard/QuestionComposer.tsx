"use client";

import { AlertTriangle, CircleCheckBig, PlayCircle, SquarePen } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useDashboardStore } from "@/stores/useDashboardStore";
import type { QuestionMode } from "@/types/dashboard";

const questionModes: Array<[QuestionMode, string]> = [
  ["choice", "选择"],
  ["short", "简答"],
  ["code", "编程"]
];

const countOptions = [5, 10, 20, 30];

export function QuestionComposer() {
  const questionMode = useDashboardStore((state) => state.questionMode);
  const difficulty = useDashboardStore((state) => state.difficulty);
  const count = useDashboardStore((state) => state.count);
  const interviewMode = useDashboardStore((state) => state.interviewMode);
  const quizState = useDashboardStore((state) => state.quizState);
  const quizWarnings = useDashboardStore((state) => state.quizWarnings);
  const draftQuestions = useDashboardStore((state) => state.draftQuestions);
  const setQuestionMode = useDashboardStore((state) => state.setQuestionMode);
  const setDifficulty = useDashboardStore((state) => state.setDifficulty);
  const setCount = useDashboardStore((state) => state.setCount);
  const setInterviewMode = useDashboardStore((state) => state.setInterviewMode);
  const generateQuestions = useDashboardStore((state) => state.generateQuestions);

  return (
    <div className="panel composer-panel">
      <div className="panel-header">
        <div>
          <h2>题单草稿</h2>
          <p>基于资料和追问策略组合问题。</p>
        </div>
        <div className="header-actions">
          {quizState === "online" && (
            <span className="sync-state online">
              <CircleCheckBig size={14} />
              已保存
            </span>
          )}
          {quizState === "offline" && (
            <span className="sync-state offline">
              <AlertTriangle size={14} />
              本地生成
            </span>
          )}
          {quizWarnings.length > 0 && (
            <div className="warnings-banner">
              <AlertTriangle size={14} />
              <div className="warnings-list">
                {quizWarnings.map((w, i) => (
                  <span key={i}>{w}</span>
                ))}
              </div>
            </div>
          )}
          <Button
            aria-busy={quizState === "syncing"}
            disabled={quizState === "syncing"}
            onClick={() => void generateQuestions()}
            variant="primary"
          >
            <PlayCircle size={17} />
            {quizState === "syncing" ? "生成中" : "重写题单"}
          </Button>
        </div>
      </div>

      <div className="composer-layout">
        <div className="controls-zone">
          <div className="field-group">
            <span className="field-label">题型</span>
            <div className="segmented-control">
              {questionModes.map(([value, label]) => (
                <button
                  className={questionMode === value ? "selected" : ""}
                  key={value}
                  onClick={() => setQuestionMode(value)}
                  type="button"
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="field-group">
            <span className="field-label">难度</span>
            <div className="slider-row">
              <input
                aria-label="题目难度"
                max="5"
                min="1"
                onChange={(event) => setDifficulty(Number(event.target.value))}
                type="range"
                value={difficulty}
              />
              <strong>{difficulty}</strong>
            </div>
          </div>

          <label className="switch-row">
            <input checked={interviewMode} onChange={(event) => setInterviewMode(event.target.checked)} type="checkbox" />
            <span />
            <strong>面试追问模式</strong>
          </label>

          <div className="field-group">
            <span className="field-label">题量</span>
            <div className="chip-row">
              {countOptions.map((opt) => (
                <button
                  key={opt}
                  className={`chip ${count === opt ? "active" : ""}`}
                  onClick={() => setCount(opt)}
                  type="button"
                >
                  {opt} 题
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="question-preview">
          {(draftQuestions ?? []).map((question, index) => (
            <article className="question-card" key={question}>
              <div className="question-index">{index + 1}</div>
              <p>{question}</p>
              <Button aria-label="编辑题目" size="small" variant="icon">
                <SquarePen size={15} />
              </Button>
            </article>
          ))}
        </div>
      </div>
    </div>
  );
}
