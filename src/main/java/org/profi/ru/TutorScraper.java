package org.profi.ru;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TutorScraper {
    public static void main(String[] args) throws InterruptedException {
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless=new");
        options.addArguments("--disable-gpu", "--no-sandbox");
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        driver.get("https://profi.ru/repetitor/english/?seamless=1&tabName=PROFILES");

        Set<String> profileLinks = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            try {
                WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Показать ещё 20']")));
                button.click();
                Thread.sleep(1000); // Даем время контенту прогрузиться
            } catch (Exception e) {
                System.out.println("Кнопка 'Показать ещё 20' не найдена или больше неактивна");
                break;
            }
        }

        // Ищем анкеты — ссылки содержат параметр profileId
        List<WebElement> cards = driver.findElements(By.xpath("//a[contains(@href, 'profileId=')]"));
        for (WebElement card : cards) {
            String href = card.getDomAttribute("href");
            if (href != null && !href.isEmpty()) {
                //обрабатываем относительные и некорректные ссылки
                if (href.startsWith("//")) {
                    href = "https:" + href;
                } else if (href.startsWith("/")) {
                    href = "https://profi.ru" + href;
                } else if (!href.startsWith("http")) {
                    href = "https://profi.ru/" + href;
                }
                profileLinks.add(href);
            }
        }

        driver.quit();
        System.out.println("Найдено ссылок на анкеты: " + profileLinks.size());

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<Void>> tasks = new ArrayList<>();

        for (String link : profileLinks) {
            tasks.add(executor.submit(() -> {
                WebDriver innerDriver = new ChromeDriver(options);
                try {
                    innerDriver.get(link);
                    String name = innerDriver.findElement(By.tagName("h1")).getText();
                    System.out.println("Имя: " + name + " | URL: " + link);
                } catch (Exception e) {
                    System.out.println("Ошибка при обработке: " + link);
                } finally {
                    innerDriver.quit();
                }
                return null;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(90, TimeUnit.MINUTES);
        System.out.println("Готово");
    }
}
