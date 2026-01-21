package com.sultan.tv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private StyledPlayerView playerView;
    private ExoPlayer player;
    private ListView listView;
    private List<Channel> channels = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> channelNames = new ArrayList<>();

    // M3U URL - Always fetches fresh token
    private static final String PLAYLIST_URL = "https://raw.githubusercontent.com/abusaeeidx/CricHd-playlists-Auto-Update-permanent/refs/heads/main/ALL.m3u";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        listView = findViewById(R.id.channel_list);

        // Setup List
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, channelNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }
        };
        listView.setAdapter(adapter);

        // Click Listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            playChannel(channels.get(position));
        });

        // Load Data
        fetchPlaylist();
    }

    private void fetchPlaylist() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(PLAYLIST_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String data = response.body().string();
                    parseM3U(data);
                }
            }
        });
    }

    private void parseM3U(String content) {
        channels.clear();
        channelNames.clear();
        String[] lines = content.split("\n");
        String name = "Unknown";
        String referer = "https://profamouslife.com/"; // Default Referer

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#EXTINF")) {
                if (line.contains(",")) {
                    name = line.substring(line.lastIndexOf(",") + 1).trim();
                }
            } else if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                referer = line.replace("#EXTVLCOPT:http-referrer=", "").trim();
            } else if (line.startsWith("http")) {
                channels.add(new Channel(name, line, referer));
                channelNames.add(name);
                // Reset defaults
                name = "Unknown";
                referer = "https://profamouslife.com/";
            }
        }

        new Handler(Looper.getMainLooper()).post(() -> adapter.notifyDataSetChanged());
    }

    private void playChannel(Channel channel) {
        releasePlayer();

        // 1. HEADER INJECTION LOGIC
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", channel.referer);
        headers.put("Origin", "https://profamouslife.com");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        HttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(headers);

        MediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channel.url));

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    // Simple Channel Model
    static class Channel {
        String name, url, referer;
        public Channel(String name, String url, String referer) {
            this.name = name;
            this.url = url;
            this.referer = referer;
        }
    }
}
