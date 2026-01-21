package com.sultan.tv;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    // আপনার দেওয়া অটো আপডেট লিংক
    private static final String PLAYLIST_URL = "https://raw.githubusercontent.com/abusaeeidx/Mrgify-BDIX-IPTV/main/playlist.m3u";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ফুল স্ক্রিন এবং স্ক্রিন অন রাখা
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        listView = findViewById(R.id.channel_list);

        // অ্যাডাপ্টার সেটআপ (লিস্ট দেখানোর জন্য)
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, channelNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE); // টেক্সট কালার সাদা
                return view;
            }
        };
        listView.setAdapter(adapter);

        // চ্যানেলে ক্লিক করলে প্লে হবে
        listView.setOnItemClickListener((parent, view, position, id) -> {
            playChannel(channels.get(position));
        });

        // প্লেলিস্ট লোড শুরু
        fetchPlaylist();
    }

    // ইন্টারনেট থেকে প্লেলিস্ট নামানো
    private void fetchPlaylist() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(PLAYLIST_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error loading playlist", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String data = response.body().string();
                    parseM3U_Advanced(data); // নতুন স্মার্ট পার্সার
                }
            }
        });
    }

    // স্মার্ট M3U পার্সার (এটি কুকি, ইউজার এজেন্ট, রেফারার সব ডিটেক্ট করে)
    private void parseM3U_Advanced(String content) {
        channels.clear();
        channelNames.clear();
        
        String[] lines = content.split("\n");
        Channel tempChannel = new Channel();
        boolean isNewChannel = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF")) {
                // আগের চ্যানেলটি সেভ করি যদি কমপ্লিট থাকে
                if (isNewChannel && tempChannel.url != null) {
                    channels.add(tempChannel);
                    channelNames.add(tempChannel.name);
                }
                
                // নতুন চ্যানেল শুরু
                tempChannel = new Channel();
                isNewChannel = true;
                
                // নাম বের করা
                if (line.contains(",")) {
                    tempChannel.name = line.substring(line.lastIndexOf(",") + 1).trim();
                } else {
                    tempChannel.name = "Unknown Channel";
                }
            } 
            // স্পেশাল হেডার ডিটেকশন (Referer)
            else if (line.startsWith("#EXTVLCOPT:http-referrer=")) {
                tempChannel.referer = line.replace("#EXTVLCOPT:http-referrer=", "").trim();
            }
            // স্পেশাল হেডার ডিটেকশন (User-Agent)
            else if (line.startsWith("#EXTVLCOPT:http-user-agent=")) {
                tempChannel.userAgent = line.replace("#EXTVLCOPT:http-user-agent=", "").trim();
            }
            // স্পেশাল হেডার ডিটেকশন (Cookie / Custom HTTP)
            else if (line.startsWith("#EXTHTTP:")) {
                try {
                    String jsonPart = line.replace("#EXTHTTP:", "").trim();
                    JsonObject jsonObject = new Gson().fromJson(jsonPart, JsonObject.class);
                    if (jsonObject.has("cookie")) {
                        tempChannel.cookie = jsonObject.get("cookie").getAsString();
                    }
                } catch (Exception e) {
                    Log.e("M3U_PARSER", "JSON Cookie parse error: " + e.getMessage());
                }
            }
            // ভিডিও লিংক পাওয়া গেলে
            else if (line.startsWith("http")) {
                tempChannel.url = line;
            }
        }
        
        // একদম শেষের চ্যানেলটি অ্যাড করা
        if (isNewChannel && tempChannel.url != null) {
            channels.add(tempChannel);
            channelNames.add(tempChannel.name);
        }

        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    // প্লেয়ার লজিক
    private void playChannel(Channel channel) {
        releasePlayer();
        
        // হেডার ম্যাপ তৈরি (যেই চ্যানেলে যা লাগবে, তাই দেওয়া হবে)
        Map<String, String> headers = new HashMap<>();
        
        // ডিফল্ট ইউজার এজেন্ট (যদি চ্যানেলে স্পেসিফিক না থাকে)
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        if (channel.userAgent != null && !channel.userAgent.isEmpty()) {
            userAgent = channel.userAgent; // Toffee এর জন্য স্পেশাল এজেন্ট
        }
        headers.put("User-Agent", userAgent);

        if (channel.referer != null && !channel.referer.isEmpty()) {
            headers.put("Referer", channel.referer);
            // কিছু লিংকে Origin হেডারও লাগে, তাই সেইফটির জন্য রেফারার কেই অরিজিন বানালাম
            String origin = channel.referer.substring(0, channel.referer.length() - 1); // remove trailing slash
            headers.put("Origin", origin);
        }

        if (channel.cookie != null && !channel.cookie.isEmpty()) {
            headers.put("Cookie", channel.cookie); // কুকি ইনজেকশন
        }

        // ExoPlayer এ হেডার সেট করা
        HttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setDefaultRequestProperties(headers);

        MediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(channel.url)));

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
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    // চ্যানেলের ডাটা রাখার ক্লাস
    static class Channel {
        String name;
        String url;
        String referer;
        String userAgent;
        String cookie;
    }
}
