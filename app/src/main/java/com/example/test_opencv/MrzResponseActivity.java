package com.example.test_opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class MrzResponseActivity extends AppCompatActivity {

    String mrz_type;
    String type;
    String country;
    String number;
    String date_of_birth;
    String expiration_date;
    String nationality;
    String sex;
    String names;
    String optional1;
    String surname;
    TextView tv_nid, tv_first_name, tv_country, tv_last_name, tv_gender, tv_dob, tv_exp;

    JSONObject jsonObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mrz_response);

        tv_first_name = (TextView) findViewById(R.id.first_name);
        tv_last_name = (TextView) findViewById(R.id.last_name);
        tv_country = (TextView) findViewById(R.id.country);
        tv_gender = (TextView) findViewById(R.id.gender);
        tv_dob = (TextView) findViewById(R.id.dob);
        tv_exp = (TextView) findViewById(R.id.exp);
        tv_nid = (TextView) findViewById(R.id.nid_number);

        try {
            jsonObject = new JSONObject(getIntent().getStringExtra("json"));

            names = jsonObject.getString("names");
            country = jsonObject.getString("country");
            date_of_birth = jsonObject.getString("date_of_birth");
            expiration_date = jsonObject.getString("expiration_date");
            sex = jsonObject.getString("sex");
            surname = jsonObject.getString("surname");
            optional1 = jsonObject.getString("optional1");

            number = jsonObject.getString("number");

            tv_nid.setText("" + number + optional1.charAt(0) + "");
            tv_first_name.setText(names);
            tv_last_name.setText(surname);
            tv_country.setText(country);
            tv_dob.setText("" + date_of_birth.charAt(4) + date_of_birth.charAt(5) + "-" + date_of_birth.charAt(2) + date_of_birth.charAt(3) + "-"  + date_of_birth.charAt(0) + date_of_birth.charAt(1));
            tv_exp.setText("" + expiration_date.charAt(4) + expiration_date.charAt(5) + "-" + expiration_date.charAt(2) + expiration_date.charAt(3) + "-"  + expiration_date.charAt(0) + expiration_date.charAt(1));
            tv_gender.setText(sex);
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Toast.makeText(Another_Activity.this, ""+jsonObject.get("Your JSON VALUE"), Toast.LENGTH_SHORT).show();

    }
}