package com.spotifyproject.yourspotify.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class Controller {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @GetMapping("/test")
    public String sendResponse() {
        return "valid";
    }

    @PostMapping("/auth/token")
    public ResponseEntity<?> getAccessToken(@RequestBody Map<String, String> body) {
        System.out.println("--------------------------------------");
        System.out.println("in getAccessToken " + body.get("code"));
        String tokenEndpoint = "https://accounts.spotify.com/api/token";
        String code = body.get("code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        System.out.println("form: " + form);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
        System.out.println("spotify token response: " + response.getBody());

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/top-tracks")
    public ResponseEntity<?> getTopTracks(@RequestHeader("Authorization") String accessToken) {
        System.out.println("--------------------------------------");
        System.out.println("in get top tracks: " + accessToken);

        String topTracksEndpoint = "https://api.spotify.com/v1/me/top/tracks?limit=10&time_range=long_term";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                topTracksEndpoint,
                HttpMethod.GET,
                request,
                Map.class
        );

        // simplifying the raw response bc it is BULKY
        // @TODO extract this simplification process into a function
        List<Map<String, String>> simplifiedResponse = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");

        for(Map<String, Object> item : items) {
            String trackName = (String) item.get("name");

            List<Map<String, Object>> artists = (List<Map<String, Object>>) item.get("artists");
            String artistNames = artists.stream()
                    .map(a -> (String) a.get("name"))
                    .collect(Collectors.joining(", "));

            System.out.println("🎵 " + trackName + " by " + artistNames);

            Map<String, String> track = new HashMap<>();
            track.put("name", trackName);
            track.put("artists", artistNames);
            simplifiedResponse.add(track);
        }

        // simplified version:  [{name: "Drake", song: "Redemption"}, {name: "Jamesy", song: "Wagwan Remix"}]
        return ResponseEntity.ok(simplifiedResponse);
    }

    @GetMapping("/top-artists")
    public ResponseEntity<?> getTopArtists(@RequestHeader("Authorization") String accessToken) {
        String topArtistsEndpoint = "https://api.spotify.com/v1/me/top/artists?limit=10&time_range=long_term";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                topArtistsEndpoint,
                HttpMethod.GET,
                request,
                Map.class
        );

        // simplifying raw response
        // @TODO extract this simplification process into a function
        List<String> simplifiedResponse = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");

        for(Map<String, Object> item: items) {
            String artistName = (String) item.get("name");
            simplifiedResponse.add(artistName);
            System.out.println("🎤 " + artistName);
        }

        // simplified version: ["Drake", "Young Thug]
        return ResponseEntity.ok(simplifiedResponse);
    }

    @GetMapping("/top-genres")
    // @TODO double work of calling /top/artists endpoint, refactor
    public ResponseEntity<?> getTopGenres(@RequestHeader("Authorization") String accessToken) {

        String topArtistsEndpoint = "https://api.spotify.com/v1/me/top/artists?limit=50&time_range=long_term";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                topArtistsEndpoint,
                HttpMethod.GET,
                request,
                Map.class
        );


        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        Map<String, Integer> genreFrequency = new HashMap<>();

        for(Map<String, Object> item: items) {
            ArrayList<String> genres = (ArrayList<String>) item.get("genres");

            for(String genre: genres) {
                genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0) + 1);
            }
        }

        // sorting in descending order
        List<Map<String, Object>> sortedGenres = genreFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> {
                    Map<String, Object> genreData = new HashMap<>();
                    genreData.put("genre", entry.getKey());
                    genreData.put("count", entry.getValue());
                    return genreData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(sortedGenres);
    }
}
