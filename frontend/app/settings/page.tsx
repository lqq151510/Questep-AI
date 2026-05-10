"use client";

import { useState, useEffect } from "react";
import {
  Save,
  Loader2,
  CheckCircle,
  Eye,
  EyeOff,
} from "lucide-react";
import { PageHero } from "@/components/new-ui/PageHero";
import {
  getLlmSettings,
  updateLlmSettings,
  toErrorMessage,
  type LlmSettingsView,
} from "@/lib/interview-api";

const providers = [
  { value: "openai", label: "OpenAI" },
  { value: "deepseek", label: "DeepSeek" },
  { value: "anthropic", label: "Anthropic (Claude)" },
  { value: "openai-compatible", label: "OpenAI 兼容（本地/自部署）" },
];

export default function ModelSettingsPage() {
  const [settings, setSettings] = useState<LlmSettingsView | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);

  const [providerName, setProviderName] = useState("openai");
  const [modelName, setModelName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    setLoading(true);
    setError("");
    try {
      const s = await getLlmSettings();
      setSettings(s);
      setProviderName(s.providerName || "openai");
      setModelName(s.modelName || "");
      setBaseUrl(s.baseUrl || "");
      setEnabled(s.enabled);
    } catch (e) {
      setError(toErrorMessage(e, "获取模型设置失败"));
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setError("");
    setSuccess(false);
    try {
      const updated = await updateLlmSettings({
        providerName,
        modelName,
        baseUrl: baseUrl || undefined,
        apiKey: apiKey || undefined,
        enabled,
      });
      setSettings(updated);
      setApiKey("");
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e) {
      setError(toErrorMessage(e, "保存模型设置失败"));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div>
        <PageHero
          kicker="设置"
          title="模型配置"
          description="配置 AI 模型的连接参数，支持 OpenAI、DeepSeek 及本地兼容模型。"
        />
        <div className="panel">
          <div className="flex items-center justify-center py-8">
            <Loader2 size={24} className="animate-spin text-[var(--blue)]" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <PageHero
        kicker="设置"
        title="模型配置"
        description="配置 AI 模型的连接参数，支持 OpenAI、DeepSeek 及本地兼容模型。"
      />

      <div className="panel">
        {error && (
          <div className="mb-4 rounded-lg bg-[var(--red-soft)] p-3 text-sm text-[var(--red)]">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 flex items-center gap-2 rounded-lg bg-[var(--green-soft)] p-3 text-sm text-[var(--green)]">
            <CheckCircle size={14} />
            模型配置已保存
          </div>
        )}

        <div className="field-group">
          <p className="field-label">模型提供商</p>
          <div className="chip-row">
            {providers.map((p) => (
              <button
                key={p.value}
                type="button"
                className={`chip ${providerName === p.value ? "active" : ""}`}
                onClick={() => setProviderName(p.value)}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        <div className="field-group">
          <p className="field-label">模型名称</p>
          <input
            type="text"
            className="input-field"
            placeholder="例如: gpt-4o, deepseek-chat, claude-3-5-sonnet-latest"
            value={modelName}
            onChange={(e) => setModelName(e.target.value)}
          />
        </div>

        <div className="field-group">
          <p className="field-label">Base URL</p>
          <input
            type="text"
            className="input-field"
            placeholder="例如: https://api.openai.com, https://api.deepseek.com"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
          />
          <p className="mt-1 text-xs text-[var(--muted)]">
            本地模型可填 http://localhost:11434/v1（Ollama）等
          </p>
        </div>

        <div className="field-group">
          <p className="field-label">
            API Key
            {settings?.hasApiKey && !apiKey && (
              <span className="ml-2 text-xs text-[var(--green)]">（已配置，留空则保留原值）</span>
            )}
          </p>
          <div className="relative">
            <input
              type={showApiKey ? "text" : "password"}
              className="input-field pr-10"
              placeholder={settings?.hasApiKey ? "留空保留已有密钥" : "输入 API Key"}
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
            />
            <button
              type="button"
              className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-[var(--muted)] hover:text-[var(--ink)]"
              onClick={() => setShowApiKey(!showApiKey)}
            >
              {showApiKey ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </div>

        <div className="field-group">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              className="h-4 w-4 rounded border-[var(--border)]"
            />
            <span className="text-sm">启用自定义模型</span>
          </label>
          <p className="mt-1 text-xs text-[var(--muted)]">
            关闭后将使用系统默认模型配置
          </p>
        </div>

        <div className="mt-6">
          <button
            type="button"
            className="btn btn-accent"
            onClick={handleSave}
            disabled={saving || !modelName.trim()}
          >
            {saving ? (
              <Loader2 size={14} className="animate-spin" />
            ) : (
              <Save size={14} />
            )}
            {saving ? "保存中..." : "保存配置"}
          </button>
        </div>

        {settings && (
          <div className="mt-4 rounded-lg bg-[var(--surface-soft)] p-3">
            <p className="text-xs text-[var(--muted)]">
              当前配置来源：{settings.source === "default" ? "系统默认" : "用户自定义"}
              {settings.providerName && ` · ${settings.providerName}`}
              {settings.modelName && ` · ${settings.modelName}`}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
