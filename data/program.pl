addToCheckList(Item) :-
    checkList(L),
    retract(checkList(L)),
    append(L, [Item], NewL),
    assert(checkList(NewL)).

municipalityWebsite('AVENTURA', 'http://www.cityofaventura.com').
municipalityWebsite('BAL HARBOUR', 'http://balharbourgov.com/').
municipalityWebsite('BAY HARBOR ISLANDS', 'https://www.bayharborislands.org/newsite/index2.asp').
municipalityWebsite('BISCAYNE PARK', 'http://biscaynepark.govoffice.com/').
municipalityWebsite('CORAL GABLES', 'http://www.coralgables.com/').
municipalityWebsite('CUTLER BAY', 'http://www.cutlerbay-fl.gov/').
municipalityWebsite('DORAL', 'http://www.cityofdoral.com/').
municipalityWebsite('EL PORTAL', 'http://www.elportalvillage.com/').
municipalityWebsite('FLORIDA CITY', 'http://www.floridacityfl.us/').
municipalityWebsite('GOLDEN BEACH', 'http://www.goldenbeach.us/').
municipalityWebsite('HIALEAH', 'http://www.hialeahfl.gov/').
municipalityWebsite('HIALEAH GARDENS', 'http://www.cityofhialeahgardens.com/cohg2/').
municipalityWebsite('HOMESTEAD', 'http://www.cityofhomestead.com/').
municipalityWebsite('INDIAN CREEK VILLAGE', 'http://www.icvps.org/index.htm').
municipalityWebsite('KEY BISCAYNE', 'http://www.keybiscayne.fl.gov/').
municipalityWebsite('MEDLEY', 'http://www.townofmedley.com/index.php').
municipalityWebsite('MIAMI', 'http://www.miamigov.com/home').
municipalityWebsite('MIAMI BEACH', 'http://web.miamibeachfl.gov/').
municipalityWebsite('MIAMI GARDENS', 'http://www.miamigardens-fl.gov/').
municipalityWebsite('MIAMI LAKES', 'http://miamilakes-fl.gov/').
municipalityWebsite('MIAMI SHORES', 'http://www.miamishoresvillage.com/').
municipalityWebsite('MIAMI SPRINGS', 'http://www.miamisprings-fl.gov/').
municipalityWebsite('NORTH BAY VILLAGE', 'http://www.nbvillage.com/').
municipalityWebsite('NORTH MIAMI', 'http://www.northmiamifl.gov/').
municipalityWebsite('NORTH MIAMI BEACH', 'http://www.citynmb.com/').
municipalityWebsite('OPA-LOCKA', 'http://www.opalockafl.gov/').
municipalityWebsite('PALMETTO BAY', 'http://www.palmettobay-fl.gov/').
municipalityWebsite('PINECREST', 'http://www.pinecrest-fl.gov/').
municipalityWebsite('SOUTH MIAMI', 'http://www.southmiamifl.gov/').
municipalityWebsite('SUNNY ISLES BEACH', 'http://www.sibfl.net/').
municipalityWebsite('SURFSIDE', 'http://www.townofsurfsidefl.gov/Surfside-Home.aspx').
municipalityWebsite('SWEETWATER', 'http://www.cityofsweetwater.fl.gov/').
municipalityWebsite('VIRGINIA GARDENS', 'http://www.virginiagardens-fl.gov/').
municipalityWebsite('WEST MIAMI', 'http://www.cityofwestmiamifl.com/').

autoClear(next(_,_)).
autoClear(form_field(_,_,_)).
