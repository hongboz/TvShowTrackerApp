package com.hongbo.projectethereal;

/**
 * Created by Hongbo on 2017-03-01.
 */

public class Episode {

    private String episodeName;
    private String episodeId;
    private String seasonNumber;
    private String episodeNumber;
    private String airDate;
    private String overview;
    private String seasonId;
    private String rating;
    private String genre;

    public String getEpisodeName() {
        return episodeName;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public String getSeasonNumber() {
        return seasonNumber;
    }

    public String getEpisodeNumber() {
        return episodeNumber;
    }

    public String getAirDate() {
        return airDate;
    }

    public String getOverview() {
        return overview;
    }

    public String getSeasonId() {
        return seasonId;
    }

    public String getRating() {
        return rating;
    }

    public void setEpisodeName(String episodeName) {
        this.episodeName = episodeName;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public void setSeasonNumber(String seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public void setEpisodeNumber(String episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public void setSeasonId(String seasonId) {
        this.seasonId = seasonId;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getGenre() {return genre;}

    public void setGenre(String genre) {this.genre = genre;}
}
