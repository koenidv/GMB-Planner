package com.koenidv.gmbplanner;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

//  Created by koenidv on 07.03.2020.
public class ListType {
    static Type COURSE = new TypeToken<ArrayList<Course>>() {
    }.getType();
    static Type CHANGE = new TypeToken<ArrayList<Change>>() {
    }.getType();
}
