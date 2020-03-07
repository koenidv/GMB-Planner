package com.koenidv.gmbplanner;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

//  Created by koenidv on 07.03.2020.
class ListType {
    static Type COURSES = new TypeToken<ArrayList<Course>>() {
    }.getType();
    static Type COURSEMAP = new TypeToken<HashMap<String, Course>>() {
    }.getType();
    static Type CHANGES = new TypeToken<ArrayList<Change>>() {
    }.getType();
}
