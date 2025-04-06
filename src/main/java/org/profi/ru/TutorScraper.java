package org.profi.ru;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TutorScraper {
    public static void main(String[] args) throws InterruptedException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("https://profi.ru/repetitor/english/?seamless=1&tabName=PROFILES");

        Set<String> profileLinks = new HashSet<>();

        for (int i = 0; i < 30; i++) {
            try {
                WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Показать ещё 20']")));
                button.click();
                Thread.sleep(2000); // Даем время контенту прогрузиться
            } catch (Exception e) {
                System.out.println("Кнопка 'Показать ещё' не найдена или больше неактивна");
                break;
            }
        }

        // Ищем анкеты — ссылки содержат параметр profileId
        List<WebElement> cards = driver.findElements(By.xpath("//a[contains(@href, 'profileId=')]"));
        for (WebElement card : cards) {
            String url = card.getAttribute("href");
            if (url != null && !url.isEmpty()) {
                profileLinks.add(url);
            }
        }

        System.out.println("Найдено ссылок на анкеты: " + profileLinks.size());
        for (String link : profileLinks) {
            System.out.println(link);
        }

        driver.quit();
    }
}
