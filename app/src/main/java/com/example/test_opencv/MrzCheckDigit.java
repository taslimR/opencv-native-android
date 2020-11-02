package com.example.test_opencv;

import android.util.Log;

import java.util.HashMap;

public class MrzCheckDigit {
    public static HashMap<Character,Integer> loadCharMap(){
        HashMap<Character, Integer> charMap = new HashMap<>();
        charMap.put('0', 0);
        charMap.put('1', 1);
        charMap.put('2', 2);
        charMap.put('3', 3);
        charMap.put('4', 4);
        charMap.put('5', 5);
        charMap.put('6', 6);
        charMap.put('7', 7);
        charMap.put('8', 8);
        charMap.put('9', 9);
        charMap.put('<', 0);
        charMap.put('A', 10);
        charMap.put('B', 11);
        charMap.put('C', 12);
        charMap.put('D', 13);
        charMap.put('E', 14);
        charMap.put('F', 15);
        charMap.put('G', 16);
        charMap.put('H', 17);
        charMap.put('I', 18);
        charMap.put('J', 19);
        charMap.put('K', 20);
        charMap.put('L', 21);
        charMap.put('M', 22);
        charMap.put('N', 23);
        charMap.put('O', 24);
        charMap.put('P', 25);
        charMap.put('Q', 26);
        charMap.put('R', 27);
        charMap.put('S', 28);
        charMap.put('T', 29);
        charMap.put('U', 30);
        charMap.put('V', 31);
        charMap.put('W', 32);
        charMap.put('X', 33);
        charMap.put('Y', 34);
        charMap.put('Z', 35);
        return charMap;
    }



    public static boolean validateMrz(String mrzValue, String checkDigit){
        HashMap<Character, Integer> charMap = loadCharMap();
        int sum = 0;
        for(int i = 0 ; i < mrzValue.length(); i++){
            int mod = i % 3;
            int intValue = charMap.get(mrzValue.charAt(i));

            if (mod == 0){
                intValue *= 7;
            }
            else if(mod ==1){
                intValue *= 3;
            }
            else{
                intValue *= 1;
            }
            sum += intValue;
        }

        int checkDigitCalculated = sum % 10;
        System.out.println(sum % 10);
        if (checkDigitCalculated == Integer.parseInt(checkDigit)){
            MainActivity.appendLog("Check digit: " + mrzValue + " for " + checkDigit + ", check digit calculated : " + checkDigitCalculated);
            return true;
        }
        else{
            MainActivity.appendLog("Check digit: " + mrzValue + " for " + checkDigit + ", check digit calculated : " + checkDigitCalculated);
            return false;
        }
    }
}
