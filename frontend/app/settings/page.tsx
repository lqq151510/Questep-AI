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
import { InlineErrorState, PageLoadingState } from "@/components/ui/PageState";
import {
  getLlmSettings,
  updateLlmSettings,
  toErrorMessage,
  type LlmSettingsView,
} from "@/lib/interview-api";

type BuiltinProvider = "openai" | "deepseek" | "anthropic" | "openai-compatible";
type ProviderOption = { value: BuiltinProvider | "custom"; label: string };

const providers: ProviderOption[] = [
  { value: "openai", label: "OpenAI" },
  { value: "deepseek", label: "DeepSeek" },
  { value: "anthropic", label: "Anthropic (Claude)" },
  { value: "openai-compatible", label: "OpenAI 兼容（本地/自部署）" },
  { value: "custom", label: "自定义厂商（OpenAI 兼容）" },
];

const providerPresets: Record<BuiltinProvider, { baseUrl: string; model: string; note: string }> = {
  openai: {
    baseUrl: "https://api.openai.com/v1",
    model: "gpt-4o-mini",
    note: "OpenAI 官方接口",
  },
  deepseek: {
    baseUrl: "https://api.deepseek.com",
    model: "deepseek-chat",
    note: "DeepSeek 官方接口（注意是 .com，不是 .co）",
  },
  anthropic: {
    baseUrl: "https://api.anthropic.com",
    model: "claude-3-5-sonnet-latest",
    note: "Anthropic 官方接口（调用路径为 /v1/messages）",
  },
  "openai-compatible": {
    baseUrl: "http://localhost:11434/v1",
    model: "qwen2.5:7b",
    note: "OpenAI 兼容接口，如 Ollama / LM Studio / SiliconFlow",
  },
};

const BUILTIN_PROVIDER_SET = new Set<BuiltinProvider>([
  "openai",
  "deepseek",
  "anthropic",
  "openai-compatible",
]);

function normalizeProviderName(raw?: string | null): string {
  if (!raw) {
    return "openai";
  }
  return raw.trim().toLowerCase().replace(/_/g, "-");
}

function isBuiltinProvider(value: string): value is BuiltinProvider {
  return BUILTIN_PROVIDER_SET.has(value as BuiltinProvider);
}

function normalizeBaseUrlForProvider(provider: string, value?: string | null): string {
  const normalized = value?.trim() ?? "";
  if (/^https:\/\/api\.deepseek\.co(\/.*)?$/i.test(normalized)) {
    if (provider !== "deepseek") {
      return normalized.replace(/^https:\/\/api\.deepseek\.co/i, "https://api.deepseek.com");
    }
    return providerPresets.deepseek.baseUrl;
  }
  return normalized;
}

export default function ModelSettingsPage() {
  const [settings, setSettings] = useState<LlmSettingsView | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);

  const [providerType, setProviderType] = useState<ProviderOption["value"]>("openai");
  const [customProviderName, setCustomProviderName] = useState("custom");
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
      const normalizedProvider = normalizeProviderName(s.providerName);
      setSettings(s);
      if (isBuiltinProvider(normalizedProvider)) {
        setProviderType(normalizedProvider);
        setCustomProviderName("custom");
      } else {
        setProviderType("custom");
        setCustomProviderName(normalizedProvider || "custom");
      }
      setModelName(s.modelName || "");
      setBaseUrl(normalizeBaseUrlForProvider(normalizedProvider, s.baseUrl));
      setEnabled(s.enabled);
    } catch (e) {
      setError(toErrorMessage(e, "获取模型设置失败"));
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    const providerName = providerType === "custom"
      ? normalizeProviderName(customProviderName)
      : providerType;
    if (!providerName) {
      setError("请输入自定义厂商标识");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess(false);
    try {
      const updated = await updateLlmSettings({
        providerName,
        modelName: modelName.trim(),
        baseUrl: baseUrl.trim() || undefined,
        apiKey: apiKey.trim() || undefined,
        enabled,
      });
      const updatedProvider = normalizeProviderName(updated.providerName);
      setSettings(updated);
      if (isBuiltinProvider(updatedProvider)) {
        setProviderType(updatedProvider);
        setCustomProviderName("custom");
      } else {
        setProviderType("custom");
        setCustomProviderName(updatedProvider || "custom");
      }
      setModelName(updated.modelName || modelName);
      setBaseUrl(updated.baseUrl || baseUrl);
      setApiKey("");
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (e) {
      setError(toErrorMessage(e, "保存模型设置失败"));
    } finally {
      setSaving(false);
    }
  };

  const activePreset = providerType === "custom" ? null : providerPresets[providerType];

  if (loading) {
    return (
      <div>
        <PageHero
          kicker="设置"
          title="模型配置"
          description="配置 AI 模型的连接参数，支持 OpenAI、DeepSeek 及本地兼容模型。"
        />
        <PageLoadingState text="正在加载模型配置..." />
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
        {error && <InlineErrorState message={error} />}

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
                className={`chip ${providerType === p.value ? "active" : ""}`}
                onClick={() => {
                  setProviderType(p.value);
                  if (p.value === "custom") {
                    return;
                  }
                  const preset = providerPresets[p.value];
                  setBaseUrl(preset.baseUrl);
                  if (!modelName.trim()) {
                    setModelName(preset.model);
                  }
                }}
              >
                {p.label}
              </button>
            ))}
          </div>
          {providerType === "custom" && (
            <div className="mt-3">
              <input
                type="text"
                className="input-field"
                placeholder="输入厂商标识，例如：siliconflow、volcengine、my-company"
                value={customProviderName}
                onChange={(e) => setCustomProviderName(e.target.value)}
              />
              <p className="mt-1 text-xs text-[var(--muted)]">
                将按 OpenAI 兼容协议调用：/chat/completions
              </p>
            </div>
          )}
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
            placeholder="例如: https://api.openai.com/v1, https://api.deepseek.com"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
          />
          <div className="mt-2 flex items-center gap-3 text-xs text-[var(--muted)]">
            <span>
              支持自定义 URL
              {activePreset ? `，当前厂商默认：${activePreset.baseUrl}` : "，可填任意 OpenAI 兼容地址"}
            </span>
            {activePreset && (
              <button
                type="button"
                className="text-[var(--blue)] hover:underline"
                onClick={() => setBaseUrl(activePreset.baseUrl)}
              >
                一键填入默认地址
              </button>
            )}
          </div>
          {activePreset && (
            <p className="mt-1 text-xs text-[var(--muted)]">{activePreset.note}</p>
          )}
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
            disabled={saving || !modelName.trim() || (providerType === "custom" && !customProviderName.trim())}
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
            <p className="mt-1 text-xs text-[var(--muted)]">
              提示：上面的切换项是待保存配置，点击“保存配置”后才会生效。
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
