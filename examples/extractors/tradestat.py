import asyncio
from playwright.async_api import async_playwright
import pandas as pd
import io

async def get_exim_data():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        # Add a user-agent to look like a real person, not a bot
        page = await browser.new_page(user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        
        url = "https://tradestat.commerce.gov.in/meidb/commoditywise_export"
        
        print("Opening page...")
        await page.goto(url, wait_until="domcontentloaded")

        # 1. Wait specifically for any dropdown to appear
        try:
            print("Waiting for Year dropdown...")
            # This uses a more generic selector to find the Year dropdown
            dropdown = await page.wait_for_selector('select:has-text("2023-2024"), select:has-text("2024-2025")', timeout=10000)
            
            # Select by value (the actual HTML value is often the year string)
            await dropdown.select_option(label="2024-2025")
            print("Year selected.")

            # 2. Select HS Code Level (2 Digit)
            await page.get_by_label("2 Digit").check()
            
            # 3. Click Submit
            await page.get_by_role("button", name="Submit").click()
            
            # 4. Extract Table
            await page.wait_for_selector('table', timeout=15000)
            content = await page.content()
            tables = pd.read_html(io.StringIO(content))
            df = max(tables, key=len)
            df.to_csv("india_pharma_exim_2024.csv", index=False)
            print("Success! Data saved.")

        except Exception as e:
            # If it fails, take a screenshot so you can see what the page looks like
            await page.screenshot(path="error_page.png")
            print(f"Failed. See 'error_page.png' for visual clue. Error: {e}")

        await browser.close()

if __name__ == "__main__":
    asyncio.run(get_exim_data())