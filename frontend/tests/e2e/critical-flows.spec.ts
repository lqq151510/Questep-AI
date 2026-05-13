import { expect, test, type Page } from "@playwright/test";

async function mockCommonApi(page: Page) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;

    if (path === "/api/v1/auth/login" && request.method() === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            token: "token-123",
            refreshToken: "refresh-123",
            tokenType: "Bearer",
          },
        }),
      });
      return;
    }

    if (path === "/api/v1/materials" && request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ success: true, data: [] }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ success: true, data: {} }),
    });
  });
}

test("login -> upload material -> generate quiz flow", async ({ page }) => {
  await mockCommonApi(page);
  await page.route("**/api/v1/materials/upload", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        data: {
          material: {
            id: 101,
            name: "demo.md",
            fileType: "MD",
            parseStatus: "SUCCESS",
            updatedAt: "2026-05-13 12:00:00",
          },
          task: {
            taskNo: "PARSE-101",
            status: "SUCCESS",
            progress: 100,
          },
        },
      }),
    });
  });
  await page.route("**/api/v1/quizzes/generate", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        data: {
          questions: [
            {
              id: 1,
              stemText: "解释 JVM 垃圾回收的分代思想",
              questionType: "SHORT_ANSWER",
              referenceAnswer: "新生代与老年代分治回收",
              optionsJson: null,
            },
          ],
        },
      }),
    });
  });

  await page.goto("/login");
  await page.getByLabel("用户名").fill("demo_user");
  await page.getByLabel("密码").fill("demo123456");
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page).toHaveURL(/\/home$/);

  await page.goto("/knowledge-base");
  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: "demo.md",
    mimeType: "text/markdown",
    buffer: Buffer.from("# demo"),
  });
  await expect(page.getByText("demo.md")).toBeVisible();

  await page.goto("/ai-test");
  await page.getByRole("button", { name: "开始测试" }).click();
  await expect(page.getByText("解释 JVM 垃圾回收的分代思想")).toBeVisible();
});

test("qa flow should recover after one failed request", async ({ page }) => {
  let chatCall = 0;
  await page.route("**/api/v1/chat", async (route) => {
    chatCall++;
    if (chatCall === 1) {
      await route.fulfill({
        status: 500,
        contentType: "application/json",
        body: JSON.stringify({ success: false, message: "boom" }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ success: true, data: { reply: "恢复成功" } }),
    });
  });

  await page.goto("/ai-qa");

  await page.getByPlaceholder("输入你的问题...").fill("第一次请求");
  await page.locator(".chat-input-row button").click();
  await expect(page.getByText(/抱歉，出了点问题/)).toBeVisible();

  await page.getByPlaceholder("输入你的问题...").fill("第二次请求");
  await page.locator(".chat-input-row button").click();
  await expect(page.getByText("恢复成功")).toBeVisible();
});

test("settings flow should save llm config", async ({ page }) => {
  await page.route("**/api/v1/llm/settings", async (route) => {
    const method = route.request().method();
    if (method === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: {
            providerName: "openai",
            modelName: "gpt-4o-mini",
            baseUrl: "https://api.openai.com/v1",
            hasApiKey: true,
            enabled: true,
            source: "user",
          },
        }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        data: {
          providerName: "deepseek",
          modelName: "deepseek-chat",
          baseUrl: "https://api.deepseek.com",
          hasApiKey: true,
          enabled: true,
          source: "user",
        },
      }),
    });
  });

  await page.goto("/settings");
  await page.getByRole("button", { name: "DeepSeek" }).click();
  await page.getByPlaceholder("例如: gpt-4o, deepseek-chat, claude-3-5-sonnet-latest").fill("deepseek-chat");
  await page.getByRole("button", { name: "保存配置" }).click();
  await expect(page.getByText("模型配置已保存")).toBeVisible();
});
