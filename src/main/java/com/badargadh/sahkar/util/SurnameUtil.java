package com.badargadh.sahkar.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SurnameUtil {
	
	public static List<String> khorajiyaNames = List.of("PATEL","BADA","DHANKANIYA","POLICE","BHADA","LAL","SAJIWALA",
			"INDAFOD","LEHRI","KADKAD","JANI","DHANGA","DHEMPUL","VANARA","BABAJI","MARKONIYA","PANJI",
			"JANI","VANARA","DHEMPUL","BHADA","LAL","SAJIWALA");
	
	public static List<String> nonsolaNames = List.of(
		    "DAIRY","MEHTA","SEKANIYA","NAGRA","BHAGAT","BHAGALIYA","CHAMACHIYA","SEKANIYA"
		);
	
	public static List<String> varaliyaNames = List.of(
		    "UKA","TITI","KUKDA","MARGA","MOOSA","AMI","MANGA","RELLI","SEKARA","SALFA","GOVIND",
		    "DAKU","LUKHA","MUKHI","USTAAD","DHABA","ZADKAL","BATAT","ZETHRA","PASHWA","VANARA",
		    "SEKARA","SALFA","GOVIND","JOSHI"
		);

	public static List<String> kadivalNames = List.of("LALA","DILIP","ANNA","NASTIK","LALA","DILIP");

	
	public static List<String> chaudhari = List.of("CHACHA","DESAI");

	public static List<String> malpara = List.of("SONAWALA","GEBI","BALWA","DALIYA","GURAKHA","BHADA",
	                               "PAHELWAN","GEBI","BALWA");

	public static List<String> nodoliya = List.of("TAKODI","MANGODI","TAKODI","BAYWALA","CHIDI",
	                                "CHAMBADIYA","BABI","FULRA","JADUGAR");

	public static List<String> manasiya = List.of("MASI");

	public static List<String> marediya = List.of("NAVISANA");

	public static List<String> sunasara = List.of("SAIDA","DHOL");
	
	public static final Map<String, String> SURNAME_REVERSE_MAP = Map.ofEntries(
		    Map.entry("KHORAJIYA", "ખોરજીયા"),
		    Map.entry("NONSOLA", "નોનસોલા"),
		    Map.entry("VARALIYA", "વરાલીયા"),
		    Map.entry("KADIVAL", "કડીવાલ"),
		    Map.entry("CHAUDHARI", "ચૌધરી"),
		    Map.entry("MALPARA", "મલપરા"),
		    Map.entry("NODOLIYA", "નોદોલીયા"),
		    Map.entry("BHORANIYA", "ભોરણીયા"),
		    Map.entry("AGLODIYA", "આગલોડીયા"),
		    Map.entry("MANASIYA", "માણસિયા"),
		    Map.entry("MAREDIYA", "મરેડીયા"),
		    Map.entry("SHERU", "શેરુ"),
		    Map.entry("SUNASARA", "સુણસરા")
	);

	public static final Map<String, String> GUJARATI_TO_ENGLISH_MAP = Map.ofEntries(
		    Map.entry("પટેલ","PATEL"),
		    Map.entry("બદા","BADA"),
		    Map.entry("ઢોકનીયા","DHANKANIYA"),
		    Map.entry("પોલીસ","POLICE"),
		    Map.entry("ભડા","BHADA"),
		    Map.entry("લાલ","LAL"),
		    Map.entry("સાજીવાળા","SAJIWALA"),
		    Map.entry("ઈંડાફોડ","INDAFOD"),
		    Map.entry("લેહરી","LEHRI"),
		    Map.entry("કડકડ","KADKAD"),
		    Map.entry("જાની","JANI"),
		    Map.entry("ઢાંગા","DHANGA"),
		    Map.entry("ઢેમપુલ","DHEMPUL"),
		    Map.entry("વનારા","VANARA"),
		    Map.entry("બાબજી","BABAJI"),
		    Map.entry("મારકોનીયા","MARKONIYA"),
		    Map.entry("પંજી","PANJI"),
		    Map.entry("ડેરી","DAIRY"),
		    Map.entry("મેહતા","MEHTA"),
		    Map.entry("શેકણીયા","SEKANIYA"),
		    Map.entry("નાગરા","NAGRA"),
		    Map.entry("ભગત","BHAGAT"),
		    Map.entry("ભાગળીયા","BHAGALIYA"),
		    Map.entry("ચમચીયા","CHAMACHIYA"),
		    Map.entry("ઉકા","UKA"),
		    Map.entry("તીતી","TITI"),
		    Map.entry("કુકડા","KUKDA"),
		    Map.entry("મરગા","MARGA"),
		    Map.entry("મૂસા","MOOSA"),
		    Map.entry("અમી","AMI"),
		    Map.entry("મંગા","MANGA"),
		    Map.entry("રેલ્લી","RELLI"),
		    Map.entry("શેકારા","SEKARA"),
		    Map.entry("સલ્ફા","SALFA"),
		    Map.entry("ગોવીંદ","GOVIND"),
		    Map.entry("ડાકુ","DAKU"),
		    Map.entry("લુખા","LUKHA"),
		    Map.entry("મુખી","MUKHI"),
		    Map.entry("ઉસ્તાદ","USTAAD"),
		    Map.entry("ઢબા","DHABA"),
		    Map.entry("ઝડકાળ","ZADKAL"),
		    Map.entry("બટાટ","BATAT"),
		    Map.entry("ઝેથરા","ZETHRA"),
		    Map.entry("પશવા","PASHWA"),
		    Map.entry("જોશી","JOSHI"),
		    Map.entry("લાલા","LALA"),
		    Map.entry("દીલીપ","DILIP"),
		    Map.entry("અન્ના","ANNA"),
		    Map.entry("નાશટીક","NASTIK"),
		    Map.entry("ચાચા","CHACHA"),
		    Map.entry("દેસાઈ","DESAI"),
		    Map.entry("સોનાવાળા","SONAWALA"),
		    Map.entry("ગેબી","GEBI"),
		    Map.entry("બાલવા","BALWA"),
		    Map.entry("ડાલીયા","DALIYA"),
		    Map.entry("ગુરખા","GURAKHA"),
		    Map.entry("પહેલવાન","PAHELWAN"),
		    Map.entry("તાકોડી","TAKODI"),
		    Map.entry("મંગોડી","MANGODI"),
		    Map.entry("બાયવાલા","BAYWALA"),
		    Map.entry("ચીડી","CHIDI"),
		    Map.entry("ચોબડીયા","CHAMBADIYA"),
		    Map.entry("બાબી","BABI"),
		    Map.entry("ફુલરા","FULRA"),
		    Map.entry("જાદુગર","JADUGAR"),
		    Map.entry("મસી","MASI"),
		    Map.entry("નાવીસના","NAVISANA"),
		    Map.entry("સૈદા","SAIDA"),
		    Map.entry("ઢોલ","DHOL"),
		    Map.entry("સાજી","SAJI")
		);

	public static final Map<String, String> ENGLISH_TO_GUJARATI_MAP =
	        GUJARATI_TO_ENGLISH_MAP.entrySet()
	            .stream()
	            .collect(Collectors.toMap(
	                Map.Entry::getValue,   // English
	                Map.Entry::getKey      // Gujarati
	            ));

	
	public static List<String> getBrnachName(String surname) {
	    switch (surname.toUpperCase()) {
	        case "KHORAJIYA": return khorajiyaNames;
	        case "NONSOLA":   return nonsolaNames;
	        case "VARALIYA":  return varaliyaNames;
	        case "KADIVAL":   return kadivalNames;
	        case "CHAUDHARI": return chaudhari;
	        case "MALPARA":   return malpara;
	        case "NODOLIYA":  return nodoliya;
	        case "MANASIYA":  return manasiya;
	        case "MAREDIYA":  return marediya;
	        case "SUNASARA":  return sunasara;
	        default:          return List.of(); // empty if not found
	    }
	}


	public static String getGujSurname(String surname) {
		return SURNAME_REVERSE_MAP.get(surname);
	}
	
	public static String getGujBranchName(String branchName) {
		return  ENGLISH_TO_GUJARATI_MAP.get(branchName);
	}
}
