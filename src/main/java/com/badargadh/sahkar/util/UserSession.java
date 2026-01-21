package com.badargadh.sahkar.util;

import com.badargadh.sahkar.data.AppUser;
import com.badargadh.sahkar.data.Member;

public class UserSession {
    // We store the Member object so we have the ID and MemberNo of the operator
    private static Member loggedInMember;
    
    private static AppUser loggedInUser;

    public static AppUser getLoggedInUser() {
        return loggedInUser;
    }
    
    public static Member getLoggedInMember() {
        return loggedInMember;
    }

    public static void setLoggedInMember(AppUser appUser) {
    	loggedInUser = appUser;
        loggedInMember = appUser.getMember();
    }

    public static void cleanUserSession() {
        loggedInMember = null;
        loggedInUser = null;
    }
}