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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class TutorScraper {
    private static final List<String> KEYWORDS = List.of("говор", "барьер", "устный", "speaking", "общен", "живой");
    private static final int MIN_PRICE = 300;
    private static final int MAX_PRICE = 5000;

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

        for (int i = 0; i < 1; i++) {
            try {
                WebElement button = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//button[normalize-space()='Показать ещё 20']")));
                button.click();
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Кнопка 'Показать ещё 20' не найдена или больше неактивна");
                break;
            }
        }

        List<WebElement> cards = driver.findElements(By.xpath("//a[contains(@href, 'profileId=')]"));
        for (WebElement card : cards) {
            String href = card.getDomAttribute("href");
            if (href != null && !href.isEmpty()) {
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
        List<String[]> filteredTutors = Collections.synchronizedList(new ArrayList<>());

        for (String link : profileLinks) {
            tasks.add(executor.submit(() -> {
                WebDriver innerDriver = new ChromeDriver(options);
                try {
                    innerDriver.get(link);

                    String name = innerDriver.findElement(By.tagName("h1")).getText();
                    String desc = innerDriver.findElement(By.tagName("body")).getText().toLowerCase();

                    boolean hasKeywords = KEYWORDS.stream().anyMatch(desc::contains);
                    if (!hasKeywords) return null;

                    List<WebElement> priceBlocks = innerDriver.findElements(By.xpath("//span[contains(text(),'\u20BD')]"));
                    for (WebElement block : priceBlocks) {
                        String text = block.getText().replaceAll("[^0-9]", "");
                        if (!text.isEmpty()) {
                            int price = Integer.parseInt(text);
                            if (price >= MIN_PRICE && price <= MAX_PRICE) {
                                System.out.println("Имя: " + name + " | Цена: " + price + " | URL: " + link);
                                filteredTutors.add(new String[]{name, String.valueOf(price), link});
                                break;
                            }
                        }
                    }
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

        try (PrintWriter writer = new PrintWriter(new FileWriter("filtered_tutors.csv"))) {
            writer.println("Name,Price,URL");
            for (String[] row : filteredTutors) {
                writer.printf("%s,%s,%s%n", row[0], row[1], row[2]);
            }
        } catch (Exception e) {
            System.out.println("Ошибка при записи в CSV: " + e.getMessage());
        }

        System.out.println("Готово. Отфильтрованные данные сохранены в filtered_tutors.csv");
    }
}

