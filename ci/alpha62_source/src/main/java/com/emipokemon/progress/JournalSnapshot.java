package com.emipokemon.progress;

import java.util.ArrayList;
import java.util.List;

public final class JournalSnapshot {
    public String questTrack = QuestDefinition.PROGRESSION;
    public long balance;
    public QuestView quest;
    public int completedQuests;
    public int totalQuests;
    public int totalAllQuests;
    public int claimableQuests;
    public List<ChapterView> chapters = new ArrayList<>();
    public int activeJobCount;
    public int maxActiveJobs;
    public List<JobView> jobs = new ArrayList<>();

    public static final class QuestView {
        public String id;
        public String chapter;
        public String chapterTitle;
        public String title;
        public String description;
        public String objective;
        public long progress;
        public long target;
        public long coins;
        public boolean complete;
        public boolean claimed;
        public List<String> items = new ArrayList<>();
    }

    public static final class ChapterView {
        public String id;
        public String title;
        public int complete;
        public int total;
        public boolean unlocked;
    }

    public static final class JobView {
        public String id;
        public String name;
        public String description;
        public int level;
        public long xp;
        public long levelStart;
        public long nextLevel;
        public boolean active;
    }
}
