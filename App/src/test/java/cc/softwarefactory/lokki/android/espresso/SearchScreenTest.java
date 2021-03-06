package cc.softwarefactory.lokki.android.espresso;


import android.util.Log;
import android.view.KeyEvent;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.json.JSONException;

import java.util.concurrent.TimeoutException;

import cc.softwarefactory.lokki.android.MainApplication;
import cc.softwarefactory.lokki.android.R;
import cc.softwarefactory.lokki.android.espresso.utilities.MockJsonUtils;
import cc.softwarefactory.lokki.android.espresso.utilities.RequestsHandle;
import cc.softwarefactory.lokki.android.espresso.utilities.TestUtils;
import cc.softwarefactory.lokki.android.services.DataService;
import cc.softwarefactory.lokki.android.utilities.ServerApi;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class SearchScreenTest extends LoggedInBaseTest{
    private static String TAG = "SearchScreenTest";


    @Override
    public void setUp() throws Exception {
        super.setUp();
        getMockDispatcher().setGetContactsResponse(new MockResponse().setResponseCode(200));
    }

    @Override
    public void tearDown() throws Exception {
        goBackN();
        super.tearDown();
    }

    /**
     * Helper method to properly remove all activities between tests
     * This is required because otherwise the next test may be unable to launch a new MainActivity instance, causing tests to fail
     * See: http://stackoverflow.com/questions/22592018/getactivity-call-causes-runtimeexception-could-not-launch-intent-intent-act-a
     */
    private void goBackN() {
        final int N = 10; // how many times to hit back button
        try {
            for (int i = 0; i < N; i++)
                pressBack();
        } catch (Exception e) {
            Log.e(TAG, "Closed all activities", e);
        }
    }

    /**
     * Go to places screen and back to force the app to load places
     */
    private void forcePlacesLoad() {
        getActivity();
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.places)).perform((click()));
        TestUtils.toggleNavigationDrawer();
        onView(withText(R.string.map)).perform((click()));
    }

    /**
     * Helper method to ensure that places are loaded before searching, otherwise tests may fail randomly
     * @throws InterruptedException
     */
    private void waitForPlaces() throws InterruptedException {
        int counter = 0;    //If we still don't have places after 1 second, let the test fail
        forcePlacesLoad();
        while (counter < 10 && MainApplication.places.toString().equals("{}")){
            counter++;
            Log.d(TAG, "No places, waiting");
            Thread.sleep(100);
        }
    }

    /**
     * Helper method to enter a search query into the search bar
     * @param query
     * @throws InterruptedException
     */
    private void enterQuery(String query) throws InterruptedException{
        onView(withId(R.id.search)).perform(click());
        onView(withId(R.id.search_src_text)).perform(clearText(), typeText(query), pressKey(KeyEvent.KEYCODE_ENTER));

        // Without this we get "PerformException: Error performing 'single click' on view".
        // See https://code.google.com/p/android-test-kit/issues/detail?id=44
        Thread.sleep(1000);
    }

    public void testSearchIconIsDisplayed(){
        getActivity();
        onView(withId(R.id.search)).check(matches(isDisplayed()));
    }

    public void testSearchNotFound() throws InterruptedException{
        getActivity();
        enterQuery("invalid_search_qwertyuiop");
        onView(withText(getResources().getString(R.string.no_search_results))).check(matches(isDisplayed()));
    }

    /* TODO: figure out why this fails on some phones even though it works when the app is run normally
    public void testClickNotFoundReturnsToMap() throws InterruptedException{
        getActivity();
        enterQuery("test");
        onView(withText(getResources().getString(R.string.no_search_results))).perform(click());
        Thread.sleep(1000);
        //If the map, we're back in the map screen
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }*/

    public void testSearchFindsContacts() throws InterruptedException, JSONException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        getActivity();
        enterQuery("example");

        onView(withText("family.member@example.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@example.com")).check(matches(isDisplayed()));
    }

    public void testSearchFindsOnlyMatchingContacts() throws InterruptedException, JSONException {
        String firstContactEmail = "family.member@example.com";
        String secondContactEmail = "work.buddy@example.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));

        getActivity();
        enterQuery("family");
        onView(withText("family.member@example.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@example.com")).check(doesNotExist());
    }

    public void testSearchFindsPlaces() throws InterruptedException, JSONException, TimeoutException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        getActivity();
        waitForPlaces();

        enterQuery("test");
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(matches(isDisplayed()));
    }

    public void testSearchFindsOnlyMatchingPlaces() throws InterruptedException, JSONException, TimeoutException {
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        getActivity();
        waitForPlaces();

        enterQuery("1");
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(doesNotExist());
    }

    public void testSearchFindsContactsAndPlaces() throws InterruptedException, JSONException, TimeoutException {

        String firstContactEmail = "family.member@test.com";
        String secondContactEmail = "work.buddy@test.com";
        getMockDispatcher().setDashboardResponse(new MockResponse().setBody(MockJsonUtils.getDashboardJsonWithContacts(firstContactEmail, secondContactEmail)));
        getMockDispatcher().setPlacesResponse(new MockResponse().setBody(MockJsonUtils.getPlacesJson()));

        getActivity();
        waitForPlaces();

        enterQuery("test");
        onView(withText("family.member@test.com")).check(matches(isDisplayed()));
        onView(withText("work.buddy@test.com")).check(matches(isDisplayed()));
        onView(withText("Testplace1")).check(matches(isDisplayed()));
        onView(withText("Testplace2")).check(matches(isDisplayed()));
    }
}
