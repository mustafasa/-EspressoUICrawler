package com.example.arifm2.expressouicrawler;

import android.support.test.rule.ActivityTestRule;

import com.example.arifm2.expressouicrawler.crawler.Crawler;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

import java.util.Locale;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by arifm2 on 9/19/2017.
 */
@RunWith(Theories.class)
public class TestCrawler {

    @Rule
    public ActivityTestRule<MainActivity> intentsTestRule = new ActivityTestRule<>(
            MainActivity.class);

    @Rule
    public ActivityTestRule<SecondActivity> intentsTestRule2 = new ActivityTestRule<>(
            SecondActivity.class);

    public static final String LANGUAGES_SET_TAG = "languageSet";

    //This is the only issue i have can't loop through datapoint in setupclass
    @DataPoints(LANGUAGES_SET_TAG)
    public static final String[] languages = {"ar", "zh", "us"};

    static Crawler crawler;

    @BeforeClass
    public static void setUpclass() throws Exception {
        crawler = new Crawler(R.string.class,R.string.class.getFields());
        crawler.setLocale(new Locale("ar"));
    }


    @Test
    public void test_MainActivity()
            throws Throwable {
        crawler.setActivity(intentsTestRule.getActivity(), R.layout.activity_main, true);
        crawler.capture();
        onView(withId(R.id.button)).perform(click());
        crawler.capture();
    }

    @Test
    public void test_Main2Activity()
            throws Throwable {
        crawler.setActivity(intentsTestRule2.getActivity(), R.layout.activity_main2, false);
        crawler.capture();
    }

    @AfterClass
    public static void tearDown() {
        crawler.captureCompleted();
    }
}
