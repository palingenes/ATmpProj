import asyncio
import os
from playwright.async_api import async_playwright
from bs4 import BeautifulSoup

BASE_URL = "https://www.shushenge.com"
NOVEL_ID = "66053"
INDEX_URL = f"{BASE_URL}/{NOVEL_ID}/index.html"
OUTPUT_FILE = "神墓.txt"

async def main():
    async with async_playwright() as p:
        # 启动浏览器（headless=False 可看到操作过程，调试用；成功后可设为 True 隐藏）
        browser = await p.chromium.launch(headless=False)
        context = await browser.new_context(
            viewport={"width": 1280, "height": 800},
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        )
        page = await context.new_page()

        print("📚 正在加载目录页（可能需要等待 Cloudflare 验证）...")
        await page.goto(INDEX_URL)

        # ✅ 关键：等待目录页的章节列表出现（根据你提供的 HTML，用 <dd> 判断）
        try:
            await page.wait_for_selector('dd a[href]', timeout=60000)  # 等最多 60 秒
            print("✅ 目录页加载成功，正在解析章节列表...")
        except Exception as e:
            print("❌ 目录页加载失败，可能是网络或反爬问题")
            html = await page.content()
            print("当前页面前500字符：", html[:500])
            await browser.close()
            return

        # 获取完整 HTML 并解析
        index_html = await page.content()
        soup = BeautifulSoup(index_html, 'html.parser')

        # 提取所有章节链接（匹配你提供的结构：<dd><a href="/413477/7.html">第一章</a></dd>）
        chapters = []
        for dd in soup.find_all('dd'):
            a = dd.find('a', href=True)
            if a and a.get_text(strip=True):
                href = a['href']
                title = a.get_text(strip=True)
                # 构造完整 URL
                if href.startswith('/'):
                    full_url = BASE_URL + href
                elif href.startswith('./'):
                    full_url = f"{BASE_URL}/{NOVEL_ID}/{href[2:]}"
                else:
                    continue
                chapters.append((full_url, title))

        print(f"✅ 共找到 {len(chapters)} 个章节")

        if not chapters:
            print("⚠️ 未提取到任何章节，请检查页面结构")
            await browser.close()
            return

        # 开始逐章抓取
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            for i, (url, title) in enumerate(chapters, 1):
                print(f"[{i}/{len(chapters)}] 正在抓取: {title}")
                try:
                    await page.goto(url, timeout=60000)
                    # ✅ 现在才等 #content（章节正文）
                    await page.wait_for_selector('#content', timeout=30000)
                    content_html = await page.content()
                    content_soup = BeautifulSoup(content_html, 'html.parser')
                    content_div = content_soup.select_one('#content')

                    if content_div:
                        # 清理广告、脚本等
                        for bad in content_div.select('script, style, div, center, .ad, .ads'):
                            bad.decompose()
                        text = content_div.get_text('\n', strip=True)
                        f.write(f"{title}\n{'='*40}\n{text}\n\n")
                    else:
                        f.write(f"{title}\n{'='*40}\n⚠️ 正文未找到\n\n")
                        print(f"  ⚠️ 未找到正文: {title}")
                except Exception as e:
                    error_msg = f"❌ 抓取失败: {title} | {str(e)[:100]}"
                    print(error_msg)
                    f.write(f"{title}\n{'='*40}\n{error_msg}\n\n")

                # 礼貌延迟，避免被封
                await asyncio.sleep(1.5)

        await browser.close()
        print(f"\n🎉 全部完成！小说已保存至:\n{os.path.abspath(OUTPUT_FILE)}")

if __name__ == "__main__":
    asyncio.run(main())