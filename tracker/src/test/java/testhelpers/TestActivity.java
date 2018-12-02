package testhelpers;

import android.app.Activity;
import android.os.Bundle;

public class TestActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getTestTitle());
    }

    public static String getTestTitle(){
        return "Test Activity";
    }
}
