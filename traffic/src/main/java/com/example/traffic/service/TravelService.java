package com.example.traffic.service;


import com.example.traffic.util.SeleniumHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class TravelService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${google.api.key}")
    private String googleApiKey;

    @Value("${openweather.api.key}")
    private String openWeatherApiKey;

    @Value("${tour.api.key}")
    private String tourApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 국내 여행지 추천 API 호출
    public String getDomesticCityRecommendation(Map<String, String> requestData) {
        String prompt = generateDomesticPrompt(requestData);
        return callChatGPT(prompt);
    }

    // 국제 여행지 추천 API 호출
    public String getInternationalCityRecommendation(Map<String, String> requestData) {
        String prompt = generateInternationalPrompt(requestData);
        return callChatGPT(prompt);
    }

    // 국내 여행 정보 (관광지, 식당, 호텔) 가져오기
    public Map<String, Object> getDomesticTravelInfo(String cityName) {
        Map<String, Object> response = new HashMap<>();
        response.put("cityName", cityName);
        Map<String, Double> latLong = getLatLong(cityName);
        if (latLong != null) {
            response.put("tourInfo", getTourInfo(latLong.get("lat"), latLong.get("lon")));
            response.put("restaurants", getRestaurants(cityName));
            response.put("hotels", getHotels(cityName));
        }
        return response;
    }

    // 국제 여행 정보 (날씨, 식당, 호텔) 가져오기
    public Map<String, Object> getInternationalTravelInfo(String cityName, Map<String, String> requestData) {
        Map<String, Object> response = new HashMap<>();
        response.put("cityName", cityName);
        Map<String, Double> latLong = getLatLong(cityName);
        if (latLong != null) {
            response.put("weatherData", getWeatherData(latLong.get("lat"), latLong.get("lon"), requestData));
            response.put("restaurants", getRestaurants(cityName));
            response.put("hotels", getHotels(cityName));
        }
        return response;
    }

    // Selenium을 이용한 항공권 예약
    public void bookFlight(String departureCity, String cityName, String startDate, String endDate) {
        SeleniumHelper seleniumHelper = new SeleniumHelper();
        seleniumHelper.bookFlight(departureCity, cityName, startDate, endDate);
    }

    // OpenAI의 ChatGPT API 호출
    private String callChatGPT(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> data = new HashMap<>();
        data.put("model", "gpt-3.5-turbo");
        data.put("messages", new Object[]{Map.of("role", "user", "content", prompt)});
        data.put("max_tokens", 150);
        // 응답을 Map<String, Object>로 받고 choices는 List<Map<String, Object>>로 캐스팅
        Map<String, Object> response = restTemplate.postForObject(url, data, Map.class, openAiApiKey);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        return content;

    }

    // 구글 API로 위도/경도 정보 가져오기
    private Map<String, Double> getLatLong(String cityName) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + cityName + "&key=" + googleApiKey;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, Double> latLong = new HashMap<>();
        if (response != null && ((List<Map<String, Object>>) response.get("results")).size() > 0) {
            Map<String, Object> location = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((List<Map<String, Object>>) response.get("results")).get(0)).get("geometry")).get("location");
            latLong.put("lat", (Double) location.get("lat"));
            latLong.put("lon", (Double) location.get("lng"));
        }
        return latLong;
    }

    // 구글 Places API를 이용해 식당 정보 가져오기
    private List<Map<String, String>> getRestaurants(String cityName) {
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=restaurants+in+" + cityName + "&key=" + googleApiKey + "&language=ko";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        return extractPlacesInfo(results);
    }

    // 구글 Places API를 이용해 호텔 정보 가져오기
    private List<Map<String, String>> getHotels(String cityName) {
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=hotels+in+" + cityName + "&key=" + googleApiKey + "&language=ko";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        return extractPlacesInfo(results);
    }

    // OpenWeather API로 날씨 정보 가져오기
    private List<Map<String, Object>> getWeatherData(Double lat, Double lon, Map<String, String> requestData) {
        String startDate = requestData.get("start_date");
        String endDate = requestData.get("end_date");
        long startTimestamp = convertToUnixTimestamp(startDate);
        long endTimestamp = convertToUnixTimestamp(endDate);

        String url = "https://history.openweathermap.org/data/2.5/history/city?lat=" + lat + "&lon=" + lon + "&type=hour&start=" + startTimestamp + "&end=" + endTimestamp + "&appid=" + openWeatherApiKey + "&lang=kr";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        return extractWeatherData((List<Map<String, Object>>) response.get("list"));
    }

    // 한국관광공사 API를 통해 관광지 정보 가져오기
    private List<Map<String, String>> getTourInfo(Double lat, Double lon) {
        String url = "http://apis.data.go.kr/B551011/KorService1/locationBasedList1?serviceKey=" + tourApiKey + "&numOfRows=10&pageNo=1&MobileOS=ETC&MobileApp=AppTest&mapX=" + lon + "&mapY=" + lat + "&radius=7000&contentTypeId=12&_type=json";
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, Object> body = (Map<String, Object>) ((Map<String, Object>) response.get("response")).get("body");
        return (List<Map<String, String>>) ((Map<String, Object>) body.get("items")).get("item");
    }

    // 도시에 대한 국내 여행지 추천 프롬프트 생성
    private String generateDomesticPrompt(Map<String, String> requestData) {
        return "Recommend a specific city in South Korea for someone traveling with " + requestData.get("companions") + ". "
                + "They will be departing from " + requestData.get("departure_city") + " and will be traveling from "
                + requestData.get("start_date") + " to " + requestData.get("end_date") + ". They prefer to use "
                + requestData.get("transportation") + " and enjoy " + requestData.get("style") + " style trips. "
                + "Only recommend a city, not a province or a large region.";
    }

    // 도시에 대한 국제 여행지 추천 프롬프트 생성
    private String generateInternationalPrompt(Map<String, String> requestData) {
        return "You are a travel assistant. Recommend a specific city that fits the following conditions, and only return the city name (no other information): "
                + "Budget: " + requestData.get("budget") + " KRW, Traveler: " + requestData.get("age") + "-year-old " + requestData.get("gender") + " traveling with " + requestData.get("companions") + ". "
                + "Travel dates: From " + requestData.get("start_date") + " to " + requestData.get("end_date") + ". Preferences: The traveler prefers " + requestData.get("preference") + " type of destinations. "
                + "Departure city: " + requestData.get("departure_city") + ".";
    }

    // Unix timestamp로 변환
    private long convertToUnixTimestamp(String date) {
        return java.time.LocalDate.parse(date).atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond();
    }

    // 날씨 데이터를 정리해서 반환
    private List<Map<String, Object>> extractWeatherData(List<Map<String, Object>> rawData) {
        // 날짜별로 데이터를 그룹화할 맵
        Map<String, List<Map<String, Object>>> dailyWeather = new HashMap<>();

        // 시간별 데이터를 받아와서 날짜별로 그룹화
        for (Map<String, Object> entry : rawData) {
            long dt = (long) entry.get("dt");
            String date = java.time.Instant.ofEpochSecond(dt)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .toString(); // 날짜를 YYYY-MM-DD 형식으로 변환

            Map<String, Object> main = (Map<String, Object>) entry.get("main");
            double temperature = (double) main.get("temp");
            double feelsLike = (double) main.get("feels_like");
            int humidity = (int) main.get("humidity");
            double rain = entry.containsKey("rain") ? ((Map<String, Double>) entry.get("rain")).getOrDefault("1h", 0.0) : 0.0;

            String weatherDescription = (String) ((Map<String, Object>) ((List<Map<String, Object>>) entry.get("weather")).get(0)).get("description");

            // 데이터 항목 생성
            Map<String, Object> weatherData = new HashMap<>();
            weatherData.put("temperature", temperature);
            weatherData.put("feels_like", feelsLike);
            weatherData.put("humidity", humidity);
            weatherData.put("rain", rain);
            weatherData.put("weather_description", weatherDescription);

            // 날짜별로 데이터 추가
            dailyWeather.computeIfAbsent(date, k -> new ArrayList<>()).add(weatherData);
        }

        // 각 날짜별로 평균을 계산하고 요약 데이터를 반환
        List<Map<String, Object>> weatherInfo = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : dailyWeather.entrySet()) {
            String date = entry.getKey();
            List<Map<String, Object>> dailyEntries = entry.getValue();

            double avgTemp = dailyEntries.stream().mapToDouble(e -> (double) e.get("temperature")).average().orElse(0.0);
            double avgFeelsLike = dailyEntries.stream().mapToDouble(e -> (double) e.get("feels_like")).average().orElse(0.0);
            double avgHumidity = dailyEntries.stream().mapToInt(e -> (int) e.get("humidity")).average().orElse(0.0);
            double totalRain = dailyEntries.stream().mapToDouble(e -> (double) e.get("rain")).sum();
            String mostCommonDescription = dailyEntries.stream()
                    .map(e -> (String) e.get("weather_description"))
                    .collect(Collectors.groupingBy(desc -> desc, Collectors.counting()))
                    .entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();

            // 일별 요약 데이터 생성
            Map<String, Object> dailySummary = new HashMap<>();
            dailySummary.put("date", date);
            dailySummary.put("avg_temperature", avgTemp);
            dailySummary.put("avg_feels_like", avgFeelsLike);
            dailySummary.put("avg_humidity", avgHumidity);
            dailySummary.put("total_rain", totalRain);
            dailySummary.put("weather_description", mostCommonDescription);

            weatherInfo.add(dailySummary);
        }

        return weatherInfo;
    }

    // 구글 Places API에서 결과 데이터 추출
    private List<Map<String, String>> extractPlacesInfo(List<Map<String, Object>> results) {
        return results.stream()
                .map(result -> Map.of(
                        "name", (String) result.get("name"),
                        "address", (String) result.get("formatted_address")
                ))
                .collect(Collectors.toList());
    }
}
