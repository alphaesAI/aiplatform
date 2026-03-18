import os
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin, urlparse

# Target URL
base_url = "https://annualreport.novonordisk.com/2024/services/downloads.html"
download_folder = "pharma_reports"

# Create directory if it doesn't exist
if not os.path.exists(download_folder):
    os.makedirs(download_folder)

def crawl_and_download(url):
    try:
        # 1. Get the raw HTML
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')

        # 2. Find all 'a' tags (links)
        links = soup.find_all('a', href=True)
        
        print(f"Found {len(links)} links. Filtering for documents...")

        for link in links:
            href = link['href']
            # Resolve relative URLs to absolute URLs
            full_url = urljoin(url, href)
            
            # 3. Filter for specific extensions
            if any(full_url.lower().endswith(ext) for ext in ['.pdf', '.docx', '.xlsx']):
                download_file(full_url)

    except Exception as e:
        print(f"Error crawling {url}: {e}")

def download_file(file_url):
    try:
        file_name = os.path.join(download_folder, os.path.basename(urlparse(file_url).path))
        
        # Stream the download to handle large PDFs efficiently
        with requests.get(file_url, stream=True) as r:
            r.raise_for_status()
            with open(file_name, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
        print(f"Successfully downloaded: {file_name}")
    except Exception as e:
        print(f"Failed to download {file_url}: {e}")

if __name__ == "__main__":
    crawl_and_download(base_url)