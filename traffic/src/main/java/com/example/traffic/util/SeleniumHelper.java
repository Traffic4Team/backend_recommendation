package com.example.traffic.util;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.Duration;

public class SeleniumHelper {

    public void bookFlight(String departureCity, String cityName, String startDate, String endDate) {
        WebDriver driver = new ChromeDriver();
        driver.get("https://flight.naver.com/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            // 출발지 입력
            WebElement startAreaButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"__next\"]/div/main/div[2]/div/div/div[2]/div[1]/button[1]")));
            startAreaButton.click();
            WebElement searchArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("autocomplete_input__qbYlb")));
            searchArea.sendKeys(departureCity);
            WebElement finishArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("autocomplete_inner__xHAxv")));
            finishArea.click();

            // 도착지 입력
            WebElement endAreaButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"__next\"]/div/main/div[2]/div/div/div[2]/div[1]/button[2]")));
            endAreaButton.click();
            searchArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("autocomplete_input__qbYlb")));
            searchArea.sendKeys(cityName);
            finishArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("autocomplete_inner__xHAxv")));
            finishArea.click();

            // 날짜 선택 구현
            selectDate(driver, wait, startDate, true);  // 출발 날짜 선택
            selectDate(driver, wait, endDate, false);   // 도착 날짜 선택

            // 검색 버튼 클릭
            WebElement searchButton = driver.findElement(By.cssSelector("button.searchBox_search__dgK4Z"));
            searchButton.click();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // 날짜 선택 함수
    private void selectDate(WebDriver driver, WebDriverWait wait, String date, boolean isStartDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(date, formatter);

        // 네이버 항공권 달력 버튼 클릭
        WebElement dateButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath(isStartDate ? "//*[@id=\"__next\"]/div/main/div[2]/div/div/div[2]/div[2]/button[1]" : "//*[@id=\"__next\"]/div/main/div[2]/div/div/div[2]/div[2]/button[2]")));
        dateButton.click();

        // 네이버 달력에서 날짜 찾기 (연도, 월 설정 후, 해당 날짜 선택)
        String formattedMonth = localDate.format(DateTimeFormatter.ofPattern("yyyy.MM"));
        String day = String.valueOf(localDate.getDayOfMonth());

        // 달력에서 올바른 월을 선택
        WebElement calendarMonth = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("sc-dAlyuH")));
        while (!calendarMonth.getText().equals(formattedMonth)) {
            WebElement nextMonthButton = driver.findElement(By.className("sc-bdVaJa"));
            nextMonthButton.click();  // 다음 달로 이동
            calendarMonth = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("sc-dAlyuH")));
        }

        // 해당 월에서 날짜 클릭
        List<WebElement> availableDays = driver.findElements(By.cssSelector(".day"));
        for (WebElement dayElement : availableDays) {
            if (dayElement.getText().equals(day)) {
                dayElement.click();
                break;
            }
        }
    }
}
