package com.example.traffic.controller;


import com.example.traffic.service.TravelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000") // React와 CORS 연결
@RequestMapping("/api")
public class TravelController {

    @Autowired
    private TravelService travelService;

    @PostMapping("/domestic")
    public Map<String, Object> domesticSurvey(@RequestBody Map<String, String> requestData) {
        String cityName = travelService.getDomesticCityRecommendation(requestData);
        return travelService.getDomesticTravelInfo(cityName);
    }

    @PostMapping("/international")
    public Map<String, Object> internationalSurvey(@RequestBody Map<String, String> requestData) {
        String cityName = travelService.getInternationalCityRecommendation(requestData);
        return travelService.getInternationalTravelInfo(cityName, requestData);
    }

    @GetMapping("/booking_flight")
    public String bookFlight(@RequestParam String startDate, @RequestParam String endDate, @RequestParam String departureCity, @RequestParam String cityName) {
        travelService.bookFlight(departureCity, cityName, startDate, endDate);
        return "항공권 예약이 완료되었습니다.";
    }
}
