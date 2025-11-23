package com.gameengine.recording;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileRecordingStorage implements RecordingStorage {
    private BufferedWriter writer;
    private String currentPath;

    @Override
    public void openWriter(String path) throws IOException {
        this.currentPath = path;
        File file = new File(path);
        file.getParentFile().mkdirs();
        this.writer = new BufferedWriter(new FileWriter(file));
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (writer != null) {
            writer.write(line);
            writer.newLine();
        }
    }

    @Override
    public void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = null;
        }
    }

    @Override
    public Iterable<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    @Override
    public List<File> listRecordings() {
        List<File> recordings = new ArrayList<>();
        File recordingsDir = new File("recordings");
        if (recordingsDir.exists() && recordingsDir.isDirectory()) {
            File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    recordings.add(file);
                }
            }
        }
        return recordings;
    }
}