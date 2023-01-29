package com.atikulsoftware.fundrawer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    FunDrawer funDrawer;
    ImageView btn_menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        funDrawer = findViewById(R.id.drawerlayout);
        btn_menu = findViewById(R.id.btn_menu);

        btn_menu.setOnClickListener(v -> {
            funDrawer.openMenu(true);
        });


    } // OnCreate Method End Here ==============


} // Public Class End Here =====================