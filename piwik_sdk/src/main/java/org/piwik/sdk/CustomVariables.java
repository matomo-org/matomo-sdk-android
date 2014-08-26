package org.piwik.sdk;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class CustomVariables extends HashMap<String, List<String>> {
    /**
     * You can track up to 5 custom variables for each user to your app,
     * and up to 5 custom variables for each screen view.
     *
     * Desired json output:
     *      {
     *          "1":["OS","iphone 5.0"],
     *          "2":["Piwik Mobile Version","1.6.2"],
     *          "3":["Locale","en::en"],
     *          "4":["Num Accounts","2"],
     *          "5":["Level","over9k"]
     *      }
     */
    private static final int MAX_VARIABLES = 5;

    public CustomVariables(){
        super(MAX_VARIABLES);
    }

    public List<String> put(int index, String name, String value){
        if (index > 0 && index <= MAX_VARIABLES) {
            return put(Integer.toString(index), Arrays.asList(name, value));
        }
        return null;
    }

    @Override
    public List<String> put(String key, List<String> value) {
        if(value.size() == 2 && key != null) {
            return super.put(key, value);
        }
        return null;
    }

    @Override
    public String toString(){
        if (size() == 0) {
            return null;
        }
        return new JSONObject(this).toString();
    }

}
