# FunDrawer
Fun Drawer with Awesome Animation

> Step 1. Add the JitPack repository to your build file 
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

> Step 2. Add the dependency
```
	dependencies {
	        implementation 'com.github.AtikulSoftware:FunDrawer:1.0.0'
	}
```

> Setp 3. Important : If show any Worning you have to add this line in gradle.properties
```
android.enableJetifier=true
```

> Step 4. Xml Design
```
    <com.atikulsoftware.fundrawer.FunDrawer
        android:id="@+id/drawerlayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipChildren="false"
        android:clipToPadding="false"
        app:edPosition="1"
        app:edMenuSize="260dp"
        app:edMenuBackground="#dddddd">

        <!--content-->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical"
            >
            <Button
                android:id="@+id/opend"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Open Drawer"
                />

        </LinearLayout>

        <!--menu-->
        <com.atikulsoftware.fundrawer.FunMenuLayout
            android:id="@+id/menulayout"
            android:layout_width="match_parent"
            android:layout_height="match_parent">

                <RelativeLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    >
                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Fun Drawer"
                        />

                </RelativeLayout>

        </com.atikulsoftware.fundrawer.FunMenuLayout>

    </com.atikulsoftware.fundrawer.FunDrawer>
```
