/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genldml.cpp
*/

#include <time.h>
#include <stdio.h>
#include <string.h>
#include "genldml.h"
#include <stdlib.h>

#define MAX_DIGITS 10
#ifndef GENLDML_NOSETAPPDATA
/*
 *  Resource Data Reference.  The data is packaged as a dll (or .so or
 *           whatever, depending on the platform) that exports a data
 *           symbol.  The application (that's us) references that symbol,
 *           here, and will pass the data address to ICU, which will then
 *           be able to fetch resources from the data.
 */

extern "C" {
	extern const void U_IMPORT *genldml_resources_dat;
}

#endif

#define XML_END_SLASH "/"
enum
{
    HELP1,
    HELP2,
    SOURCEDIR,
	DESTDIR,
	PACKAGE_NAME,
};


UOption options[]={
                      UOPTION_HELP_H,
                      UOPTION_HELP_QUESTION_MARK,
                      UOPTION_SOURCEDIR,
					  UOPTION_DESTDIR,
					  UOPTION_DEF("package", 'p', UOPT_REQUIRES_ARG)
                   };

const char* usageString =
                "Usage: genldml [OPTIONS] [FILES]\n"
                "\tConvert the input res file to XML\n"
                "Options:\n"
                "\t-h or -? or --help       this usage text\n"
                "\t-s or --sourcedir        source directory for files followed by path\n"
                "\t                         followed by path\n"
                "\t-d or --destdir          dest directory for files followed by path\n"
                "\t                         followed by path\n"
				"\t-p or --package			name of the package that is prepended to the resource bundles\n"
				"\t                         defaults to ICU's data\n"
               ;

int main (int32_t argc, const char* argv[]) {
	const char* srcDir =NULL;
	const char* destDir = NULL;
	const char* packageName=NULL;
	UErrorCode err= U_ZERO_ERROR;
	char* path= NULL;
	//initialize the argument list
    U_MAIN_INIT_ARGS(argc, argv);
    //parse the arguments
    int32_t _remainingArgc = u_parseArgs(argc, (char**)argv, (int32_t)(sizeof(options)/sizeof(options[0])), options);

    // Now setup the arguments
    if(argc==1 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
        fprintf(stderr,usageString);
        return -1;
    }

    if(options[SOURCEDIR].doesOccur) {
        srcDir = options[SOURCEDIR].value;
    }
    if(options[DESTDIR].doesOccur) {
        destDir = options[DESTDIR].value;
    }
	if(options[PACKAGE_NAME].doesOccur) {
        packageName = options[PACKAGE_NAME].value;
    }
#ifndef GENLDML_NOSETAPPDATA
    /* Tell ICU where our resource data is located in memory.
     *   The data lives in the genldml_resources dll, and we just
     *   pass the address of an exported symbol from that library
     *   to ICU.
     */
    udata_setAppData("genldml_resources", &genldml_resources_dat, &err);
    if (U_FAILURE(err)) {
        fprintf(stderr, "%s: ures_open failed with error \"%s\"\n", argv[0], u_errorName(err));
        exit(-1);
    }
#endif
	if(srcDir!=NULL){
		path = (char*) malloc(strlen(srcDir)+((packageName!=NULL) ? strlen(packageName) : 0 )+2);
		strcpy(path,srcDir);
		if(path[(strlen(srcDir)-1)]!=U_FILE_SEP_CHAR){
			strcat(path,U_FILE_SEP_STRING);
		}
		if(packageName!=NULL){
			strcat(path,packageName);
		}
	}
    if(_remainingArgc<0) {
        fprintf(stderr,usageString);
        return -1;
    }
	for(int i=1; i<_remainingArgc; i++){

		GenerateXML gen(path,argv[i],destDir,err);
		if(U_FAILURE(err)){
			fprintf(stderr,"Reading of resource file failed. Error: %s\n",u_errorName(err));
			return -1;
		}
		gen.DoIt();
		
	}
	free(path);
	return 0;
}

static int32_t
itou (UChar * buffer, uint32_t i, uint32_t radix, int32_t pad)
{
    int32_t length = 0;
    int32_t num = 0;
    int digit;
    int32_t j;
    UChar temp;

    do{
        digit = (int)(i % radix);
        buffer[length++]=(UChar)(digit<=9?(0x0030+digit):(0x0030+digit+7));
        i=i/radix;
    } while(i);

    while (length < pad){
        buffer[length++] = (UChar) 0x0030;/*zero padding */
    }
    /* null terminate the buffer */
    if(length<MAX_DIGITS){
        buffer[length] = (UChar) 0x0000;
    }
    num= (pad>=length) ? pad :length;

    /* Reverses the string */
    for (j = 0; j < (num / 2); j++){
        temp = buffer[(length-1) - j];
        buffer[(length-1) - j] = buffer[j];
        buffer[j] = temp;
    }
    return length;
}


GenerateXML::GenerateXML(const char* path, const char* locName,const char* destDir, UErrorCode& err)
						 :  mSourceBundle(path, locName,err), mStringsBundle("genldml_resources","root", err),mError(err){
    mFile=NULL;
    if(U_SUCCESS(err)){
       
	    mLocale=Locale(locName);
	    mFile=getFileHandle(destDir,mLocale);
        /* assume that PCD bundles and tr bundles 
         * are in a directory ul
         */
        GenerateXML::locName = locName;
        GenerateXML::path = path;
//		GenerateXML::packageName = packageName;
	    if(mFile==NULL){
            closeFileHandle();
		    exit(U_FILE_ACCESS_ERROR);
        }

	    indentOffset="";
    }

}
GenerateXML::~GenerateXML()
{

    closeFileHandle();

}

void GenerateXML::closeFileHandle(){
    if(mFile){
	    fclose(mFile);
    }
}
void GenerateXML::DoIt(){
	
	writeXMLVersionAndComments();
	writeIdentity();
	writeDisplayNames();
	writeLayout();
	writeEncodings();
	writeDelimiters();
	writeMeasurement();
	writeDates();
	writeNumberFormat();
	writeSpecial();
	writeCollations();
/*	
	writeTransliteration();
	writeBoundary();
//	writeMisc();
    writePosixAdditions();
    writePosixCompData();
*/
 	closeXMLDocument();	
}
void GenerateXML::chopIndent(){

	indentOffset.remove((indentOffset.length()-1),1);
}
int32_t GenerateXML::copyUnicodeStringToChars(const UnicodeString& str,
                                 char* buf,
                                 int32_t bufCapacity) {
    int32_t len = str.length();
    if (buf != 0) {
        // copy whatever will fit into buf
        int32_t len2 =  (len<(bufCapacity - 1))? len : (bufCapacity-1);
        str.extract(0, len2, buf, "");
        buf[len2] = 0; // zero-terminate
    }
    return len; // return actual length    
}
void GenerateXML::closeXMLDocument(){
	chopIndent();
	Formattable args[] = {UnicodeString(XML_END_SLASH)};
	UnicodeString xmlString;
	formatString(mStringsBundle.getStringEx("localeData",mError),args,1,xmlString);
	printString(&xmlString);
}
UnicodeString GenerateXML::formatString(UnicodeString& str,UnicodeString& argument,UnicodeString& result){
	Formattable args[] ={ argument};
	MessageFormat format(str,mError);
	FieldPosition fpos=0;
	result.remove();
	format.format(args,1, result,fpos,mError);
//	if(U_FAILURE(mError)) {
//		return UnicodeString("Illegal argument");
//	}

	return result;
}


UnicodeString GenerateXML::formatString(UnicodeString& str,const Formattable* args,int32_t num,UnicodeString& result){
	FieldPosition fpos=0;
	MessageFormat format(str,mError);
	result.remove();
	format.format(args,num, result,fpos,mError);
//	if(U_FAILURE(mError)) {
//		return UnicodeString("Illegal argument");
//	}
	return result;
}


FILE* GenerateXML::getFileHandle(const char* path,Locale loc)
{
	
	FILE* file;
	char fileName[256];
    if(!path){
        path=".";
    }
    if(U_FAILURE(mError) ) {
        return NULL;
    } 
	strcpy(fileName,path);
    if(path[ strlen(path)-1] !='\\'){
         strcat(fileName,"\\");
    }
	 strcat(fileName, loc.getName());
     strcat(fileName, ".xml");

    /* open the output file */
    file=fopen(fileName,"w");

    
    if(file==NULL) {
		
        mError=U_FILE_ACCESS_ERROR;
		printf("Could not open file %s for writing.\n",fileName);
		exit(U_FILE_ACCESS_ERROR);
        return NULL;
    }else{
        printf("opened file %s in directory %s\n", fileName, path);
    }

	return file;
}


int32_t 
GenerateXML::fillOutputString(const UnicodeString &temp,
                      UChar *dest, 
                      int32_t destCapacity) {
  int32_t length = temp.length();

  if (destCapacity > 0) {
    // copy the contents; extract() will check if it needs to copy anything at all
    temp.extract(0, destCapacity, dest, 0);

    // zero-terminate the dest buffer if possible
    if (length < destCapacity) {
      dest[length] = 0;
    }
  }

  // set the error code according to the necessary buffer length
  if (length > destCapacity && U_SUCCESS(mError)) {
    mError = U_BUFFER_OVERFLOW_ERROR;
  }

  // return the full string length
  return length;
}

void GenerateXML::printString(UnicodeString* uString){

	//UChar result[256];
	char *dest = NULL;
	int32_t destLen = 0;
	int32_t destCap =0;
	const UChar* src = uString->getBuffer();
    int32_t srcLen=uString->length();

	u_strToUTF8(dest,destCap, &destLen,src,srcLen,&mError);
	if(mError == U_BUFFER_OVERFLOW_ERROR){
			destCap = destLen+2;
			dest = (char*) malloc(destCap);
			mError = U_ZERO_ERROR;
			dest = u_strToUTF8(dest,destCap, &destLen,src,srcLen,&mError);
			fwrite(dest,destLen,sizeof(char),mFile);
	}

	uString->releaseBuffer();
	free(dest);
	if(U_FAILURE(mError)){
		fprintf(stderr,"Conversion of string to UTF-8 failed with error: %s\n",u_errorName(mError));
		exit(mError);
	}
}

void GenerateXML::writeXMLVersionAndComments(){
	
	UnicodeString xmlString;
	if(U_FAILURE(mError)) {
        return;
	}
	UnicodeString temp = mStringsBundle.getStringEx("declaration",mError);
    xmlString.append(temp);
	Formattable arguments[] = {""};
	UnicodeString tempStr;
	xmlString.append(formatString(mStringsBundle.getStringEx("localeData",mError),arguments,1,tempStr));
	printString(&xmlString);
    mError=U_ZERO_ERROR;
}

void GenerateXML::writeVersion(UnicodeString& xmlString){
	UnicodeString tempStr;
	UnicodeString version;
	if(U_FAILURE(mError)) {
        return;
	}
	version=mSourceBundle.getStringEx("Version",mError);
	// if version is not available provide a default version.
    if( mError == U_USING_DEFAULT_WARNING ||
		mError == U_USING_FALLBACK_WARNING || 
		mError == U_MISSING_RESOURCE_ERROR){
        version="1.0";
        mError = U_ZERO_ERROR;
    }
	Formattable args[] = {indentOffset,""};
	UnicodeString result;
	if(!version.isEmpty()){
		args[0]=indentOffset;
		args[1]=version;
       	xmlString.append(formatString(mStringsBundle.getStringEx("version",mError),args,2,result));

	}
	args[0]=indentOffset;
	args[1]= UnicodeString(XML_END_SLASH);
	args[2]="";
	xmlString.append(formatString(mStringsBundle.getStringEx("versioning",mError), args,3,result));
	//printString(&xmlString);
    mError=U_ZERO_ERROR;

}

void GenerateXML::writeIdentity(){
	UnicodeString xmlString, tempStr,tempStr1,tempStr2;
	indentOffset.append("\t");
	Formattable args[]={indentOffset,"",""};
	
	formatString(mStringsBundle.getStringEx("identity",mError),args,2,xmlString);
    
	indentOffset.append("\t");
	//version
	writeVersion(xmlString);
	//genrationDate
	SimpleDateFormat format(UnicodeString("yyyy-MM-dd"), mError);
	format.format(Calendar::getNow(),tempStr);
	args[0]=indentOffset;
    args[1]=tempStr;
	tempStr.remove();
	
	xmlString.append(formatString(mStringsBundle.getStringEx("generationDate",mError),args,2,tempStr));

	tempStr1=mLocale.getISO3Language() ;
	tempStr2=mLocale.getLanguage();
    
	if(!(tempStr1.isEmpty() && tempStr2.isEmpty())){

		Formattable args1[] = {indentOffset, tempStr2,""};
		UnicodeString t;
		xmlString.append(formatString(mStringsBundle.getStringEx("language",mError),args1,3,t));
	}
	tempStr1.remove();
	tempStr2.remove();
	tempStr1=mLocale.getISO3Country();
	tempStr2=mLocale.getCountry();

	if(!(tempStr1.isEmpty() && tempStr2.isEmpty())){
		Formattable args1[] = {indentOffset,tempStr1,tempStr2};
		UnicodeString t;
		xmlString.append(formatString(mStringsBundle.getStringEx("territory",mError),args1,3,t));
	}
	if(mError==U_MISSING_RESOURCE_ERROR){
		mError=U_ZERO_ERROR;
	}
	tempStr.remove();
	tempStr=mLocale.getVariant();
	if(!tempStr.isEmpty() && tempStr!="t"){
		Formattable args1[] = {indentOffset, tempStr};
		UnicodeString t;
		xmlString.append(formatString(mStringsBundle.getStringEx("variant",mError),args1,2,t));
	}

	ResourceBundle tb  = mSourceBundle.get("LocaleID",mError);
    UResType type = tb.getType();
	chopIndent();
	args[0]=indentOffset;
	args[1]=	(UnicodeString(XML_END_SLASH));
	args[2]= "";
    mError =U_ZERO_ERROR;
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("identity",mError),args,2,t));
	printString(&xmlString);
}

void GenerateXML::writeDisplayNames(){
	UnicodeString xmlString;
	UBool print = FALSE;
	if(U_FAILURE(mError)) {
		return;
	}
	Formattable args[]={indentOffset,"",""};
	UnicodeString t;
	xmlString= formatString(mStringsBundle.getStringEx("localeDisplayNames",mError),args,3,t);
	UnicodeString tempStr;
	indentOffset.append("\t");

	writeScript(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	writeLanguage(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	writeCountryNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	writeVariantNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	writeKeywordNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	writeTypeNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	chopIndent();

	Formattable args1[]={indentOffset,(UnicodeString(XML_END_SLASH)),""};
	xmlString.append(formatString(mStringsBundle.getStringEx("localeDisplayNames",mError) ,args1,3,t));
	if(print)  printString(&xmlString);
}

void GenerateXML::writeTable(const char* key, const char* resMain, const char* resElement, UnicodeString& xmlString){

	if(U_FAILURE(mError)) {
		return;
	}
	Formattable args[]={indentOffset,"",""};
	UnicodeString t;
	xmlString= formatString(UnicodeString(mStringsBundle.getStringEx(resMain,mError)),args,3,t);

    ResourceBundle dBundle=mSourceBundle.get(key,mError);
    if( U_SUCCESS(mError)  && 
        mError != U_USING_FALLBACK_WARNING && 
        mError != U_USING_DEFAULT_WARNING){  
		
		indentOffset.append("\t");
        while(dBundle.hasNext()){
			ResourceBundle dBundle1 = dBundle.getNext(mError);
            const char* mykey=dBundle1.getKey();
            UnicodeString string = dBundle.getStringEx(mykey,mError);
		    Formattable args1[]={indentOffset,mykey,string};
		    xmlString.append(formatString(mStringsBundle.getStringEx(resElement,mError),args1,3,t));
		    mError=U_ZERO_ERROR;
	    }
		chopIndent();
		Formattable args1[]={indentOffset,(UnicodeString(XML_END_SLASH)),""};
		xmlString.append(formatString(mStringsBundle.getStringEx(resMain,mError) ,args1,3,t));
		return;
    }
				
	mError=U_ZERO_ERROR;
	xmlString.remove();
}
void GenerateXML::writeScript(UnicodeString& xmlString){

	writeTable("Scripts","scripts","script",xmlString);		
}

void GenerateXML::writeLanguage(UnicodeString& xmlString){

	writeTable("Languages","languages","language",xmlString);
}
void GenerateXML::writeCountryNames(UnicodeString& xmlString){

	writeTable("Countries","territories","territory",xmlString);
}


void GenerateXML::writeVariantNames(UnicodeString& xmlString){
	// hard code the variant names for now
	const char* variants[] = {
								"%%EURO",						
								"%%B",   
								"%%NY",  
								"%%AL",
							};
	UnicodeString tempStr;
	Formattable args[] = { indentOffset, "",""};
	formatString(mStringsBundle.getStringEx("variants",mError),args,2,xmlString);
	
	indentOffset.append("\t");
	UnicodeString t;
	for(int i = 0; i<sizeof(variants)/sizeof(char*);i++){
		UnicodeString var(variants[i],2,strlen(variants[i]));
		args[0] = indentOffset;
		args[1] = var;
		args[2] = mSourceBundle.getStringEx(variants[i],mError);
		if(	mError != U_USING_FALLBACK_WARNING && 
			mError != U_USING_DEFAULT_WARNING && 
			U_SUCCESS(mError)){

			tempStr.append(formatString(mStringsBundle.getStringEx("variant",mError),args,3,t));
			
		}
		mError = U_ZERO_ERROR;
	}
	xmlString.append(tempStr);
	chopIndent();
	args[0] = indentOffset;
	args[1] = "/";
	xmlString.append(formatString(mStringsBundle.getStringEx("variants",mError),args,2,t));
	if(tempStr.isEmpty()){
		xmlString.remove();
	}
}

void GenerateXML::writeKeywordNames(UnicodeString& xmlString){
	// Currently the keywords we have are collation only
	// we shall fix this method later if we add more keywords

	const char* variants[] = {
								"collation",	
								"calendar",		
							};
	UnicodeString tempStr;
	Formattable args[] = { indentOffset, "","",""};
	formatString(mStringsBundle.getStringEx("keys",mError),args,2,xmlString);
	
	indentOffset.append("\t");
	UnicodeString t;
	for(int i = 0; i<sizeof(variants)/sizeof(char*);i++){
		
		args[0] = indentOffset;
		args[1] = variants[i];
		args[2] = variants[i];

		if(	mError != U_USING_FALLBACK_WARNING && 
			mError != U_USING_DEFAULT_WARNING && 
			U_SUCCESS(mError)){

			tempStr.append(formatString(mStringsBundle.getStringEx("key",mError),args,3,t));
			
		}
		mError = U_ZERO_ERROR;
	}
	xmlString.append(tempStr);
	chopIndent();
	args[0] = indentOffset;
	args[1] = "/";
	xmlString.append(formatString(mStringsBundle.getStringEx("keys",mError),args,2,t));
	if(tempStr.isEmpty()){
		xmlString.remove();
	}
}
	// hard code the variant names for now
static const char* collationVariants[] = {
								"%%PHONEBOOK",	
								"%%PINYIN",		
								"%%TRADITIONAL", 
								"%%STROKE",		
								"%%DIRECT",		
							};
void GenerateXML::writeTypeNames(UnicodeString& xmlString){

	UnicodeString tempStr;
	Formattable args[] = { indentOffset, "","",""};
    formatString(mStringsBundle.getStringEx("types",mError),args,2,xmlString);
	
	indentOffset.append("\t");
	UnicodeString t;
	for(int i = 0; i<sizeof(collationVariants)/sizeof(char*);i++){
		
		UnicodeString var(collationVariants[i],2,strlen(collationVariants[i]));
		args[0] = indentOffset;
		args[1] = var;
		args[2] = "collation"; // hard code this value for now
		args[3] = mSourceBundle.getStringEx(collationVariants[i],mError);

		if(	mError != U_USING_FALLBACK_WARNING && 
			mError != U_USING_DEFAULT_WARNING && 
			U_SUCCESS(mError)){

			tempStr.append(formatString(mStringsBundle.getStringEx("type",mError),args,4,t));
			
		}
		mError = U_ZERO_ERROR;
	}
	xmlString.append(tempStr);
	chopIndent();
	args[0] = indentOffset;
	args[1] = "/";
	xmlString.append(formatString(mStringsBundle.getStringEx("types",mError),args,2,t));
	if(tempStr.isEmpty()){
		xmlString.remove();
	}
}

void GenerateXML::writeLayout(){
	UnicodeString xmlString;
	Formattable args[] = {indentOffset,"",""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("layout", mError), args,2,t));
	UnicodeString lang = mLocale.getLanguage();
	UnicodeString country = mLocale.getCountry();
	indentOffset.append("\t");
	if(lang=="ar" || lang=="he" || lang=="il" ){
		args[0] = indentOffset;
		args[1] = "top-to-bottom";
		args[2] = "right-to-left";
		xmlString.append(formatString(mStringsBundle.getStringEx("orientation",mError), args,3,t));
	}else{
		args[0] = indentOffset;
		args[1] = "top-to-bottom";
		args[2] = "left-to-right";
		xmlString.append(formatString(mStringsBundle.getStringEx("orientation",mError), args,3,t));
	}
	chopIndent();
	args[0] = indentOffset;
	args[1] = "/";
	xmlString.append(formatString(mStringsBundle.getStringEx("layout",mError),args,2,t));
	mError = U_ZERO_ERROR;
	//print this only in the root language locale
	if(country==""|| lang=="root"){
		printString(&xmlString);
	}
	
}
void GenerateXML::writeEncodings(){
	UnicodeString xmlString;
	Formattable args[] = {indentOffset,"",""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("encodings",mError),args,2,t));
	indentOffset.append("\t");
	args[0] = indentOffset;
	args[1] = mSourceBundle.getStringEx("ExemplarCharacters",mError);
	
	if(	mError != U_USING_FALLBACK_WARNING && 
		mError != U_USING_DEFAULT_WARNING && 
		U_SUCCESS(mError)){
	
		xmlString.append(formatString(mStringsBundle.getStringEx("exemplarCharacters",mError),args,2,t));
		// currently no mappings are defined in ICU 
		// so we donot add them
		chopIndent();
		args[0] = indentOffset;
		args[1] = "/";
		xmlString.append(formatString(mStringsBundle.getStringEx("encodings",mError),args,2,t));
		printString(&xmlString);
	}
	mError = U_ZERO_ERROR;
}

void GenerateXML::writeDelimiters(){
// Data not available in ICU
}

void GenerateXML::writeMeasurement(){
// Data not available in ICU
}


void GenerateXML::writeDates(){
	UnicodeString xmlString;
	UBool print = FALSE;
	if(U_FAILURE(mError)) {
		return;
	}
	Formattable args[]={indentOffset,"",""};
	formatString(mStringsBundle.getStringEx("dates",mError),args,3,xmlString);
	UnicodeString tempStr;
	indentOffset.append("\t");
	
	writeLocalePatternChars(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	
	tempStr.remove();
	writeCalendars(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeTimeZoneNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	chopIndent();

	Formattable args1[]={indentOffset,(UnicodeString(XML_END_SLASH)),""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("dates",mError) ,args1,3,t));
	if(print)  printString(&xmlString);
}

void GenerateXML::writeTimeZoneNames(UnicodeString& xmlString){

	if(U_FAILURE(mError)) {
		return;
	}
	UBool isDefault =TRUE;
	Formattable args[4] ={indentOffset,"","",""};

	UnicodeString t;

    ResourceBundle dBundle = mSourceBundle.get("zoneStrings", mError);
	if(mError!=U_USING_DEFAULT_WARNING && U_SUCCESS(mError) && mError!=U_USING_FALLBACK_WARNING){
        
		xmlString.append(formatString(mStringsBundle.getStringEx("timeZone",mError),args,2,t));
		
		indentOffset.append("\t");
        while(dBundle.hasNext()){
            ResourceBundle dBundle1 = dBundle.getNext(mError);

            if(U_SUCCESS(mError)){
                int i = 0;
                UnicodeString data =dBundle1.getStringEx((int32_t)5,mError);
                if(U_SUCCESS(mError)){
             	     args[0] =indentOffset;
					 args[1] = data;
					 args[2] = "";
			        xmlString.append(formatString(mStringsBundle.getStringEx("zoneStart",mError),args,3,t));

			        isDefault=FALSE;
			        indentOffset.append("\t");
                    
					args[0] =indentOffset;
					args[1] = "";
                    //long format 
			        xmlString.append(formatString(mStringsBundle.getStringEx("long",mError),args,2,t));

			        indentOffset.append("\t");
         	        args[0]=indentOffset;
			        args[1]=dBundle1.getStringEx((int32_t)1,mError);
                    if(U_SUCCESS(mError)){
			            xmlString.append(formatString(mStringsBundle.getStringEx("generic",mError),args,2,t));
                    }
                    mError=U_ZERO_ERROR;
                    args[1]=dBundle1.getStringEx((int32_t)1,mError);
                    if(U_SUCCESS(mError)){
			            xmlString.append(formatString(mStringsBundle.getStringEx("standard",mError),args,2,t));
                    }
                    mError =U_ZERO_ERROR;
                    args[1]=dBundle1.getStringEx((int32_t)3,mError);
                    if(U_SUCCESS(mError)){
			            xmlString.append(formatString(mStringsBundle.getStringEx("daylight",mError),args,2,t));
                    }
                    mError=U_ZERO_ERROR;
                    chopIndent();
			        args[0]=indentOffset;
					args[1]=UnicodeString(XML_END_SLASH);
			        xmlString.append(formatString(mStringsBundle.getStringEx("long",mError),args,2,t));
                    
					args[1]="";
                    // short format 
			        xmlString.append(formatString(mStringsBundle.getStringEx("short",mError),args,2,t));

			        indentOffset.append("\t");
			        args[0]=indentOffset;
                    data=dBundle1.getStringEx((int32_t)0,mError);
                    if(U_SUCCESS(mError)){
			            args[1]=data;
			            xmlString.append(formatString(mStringsBundle.getStringEx("generic",mError),args,2,t));
                    }
                    data=dBundle1.getStringEx((int32_t)2,mError);
                    if(U_SUCCESS(mError)){
			            args[1]=data;
			            xmlString.append(formatString(mStringsBundle.getStringEx("standard",mError),args,2,t));
                    }
                    data = dBundle1.getStringEx((int32_t)4,mError);
                    if(U_SUCCESS(mError)){
    			        args[1]=data;
    			        xmlString.append(formatString(mStringsBundle.getStringEx("daylight",mError),args,2,t));
                    }
                    mError = U_ZERO_ERROR;
			        chopIndent();
			        args[0]=indentOffset;
					args[1]=UnicodeString(XML_END_SLASH);
			        xmlString.append(formatString(mStringsBundle.getStringEx("short",mError),args,2,t));
			        
			        args[1].setString(dBundle1.getStringEx((int32_t)5,mError));
                    if(U_SUCCESS(mError)){
			            xmlString.append(formatString(mStringsBundle.getStringEx("exemplarCity",mError),args,2,t));
                    }
                    mError=U_ZERO_ERROR;
			        chopIndent();
			        xmlString.append(formatString(mStringsBundle.getStringEx("zoneEnd",mError),indentOffset,t));
					

                }
		    }
            mError=U_ZERO_ERROR;

        }
		chopIndent();
		args[0]=indentOffset;
		args[1]=UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("timeZone",mError),args,2,t));
		//printString(&xmlString);
		return;
    }
	mError=U_ZERO_ERROR;
}

		
void GenerateXML::writeCalendars(UnicodeString& xmlString){
	UnicodeString tempStr;
	UBool print =FALSE;
	Formattable args[2]={indentOffset,""};
	xmlString.append(formatString(mStringsBundle.getStringEx("calendars",mError),args,2,tempStr));

	indentOffset.append("\t");
    mError=U_ZERO_ERROR;
	tempStr.remove();
	writeCalendar(UnicodeString("gregorian"),TRUE,tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	chopIndent();
	args[1]=UnicodeString(XML_END_SLASH);
	xmlString.append(formatString(mStringsBundle.getStringEx("calendars",mError),args,2,tempStr));
	
}

void GenerateXML::writeCalendar(UnicodeString& calendar,UBool isDefault,UnicodeString& xmlString){
	
	UnicodeString tempStr;
	UBool print =FALSE;
	Formattable args[]={indentOffset,calendar};
	// hard code default to medium pattern for ICU
	xmlString.append(formatString(mStringsBundle.getStringEx( "default",mError),args,2,tempStr));
	xmlString.append(formatString(mStringsBundle.getStringEx("calendarStart",mError),args,2,tempStr));

	indentOffset.append("\t");
	tempStr.remove();
	writeMonthNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeMonthAbbr(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeDayNames(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeDayAbbr(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeWeek(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeEra(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeAMPMmarkers(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeDateFormat(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
    tempStr.remove();
	writeTimeFormat(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
    tempStr.remove();
	writeDateTimeFormat(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	chopIndent();
    mError =U_ZERO_ERROR;
	xmlString.append(formatString(mStringsBundle.getStringEx("calendarEnd",mError),indentOffset,tempStr));
}

const char* getDayName(int index){
	switch(index){
	case 0:
		return "sun";
	case 1:
		return "mon";
	case 2:
		return "tue";
	case 3:
		return "wed";
	case 4:
		return "thu";
	case 5:
		return "fri";
	case 6:
		return "sat";
	default:
		return "";
	}
}
void GenerateXML::writeMonthNames(UnicodeString& xmlString){

    ResourceBundle longMonths= mSourceBundle.get("MonthNames",mError);
	UnicodeString t;

	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("monthNames",mError),args,2,t));
		indentOffset.append("\t");
        for(int i=0;longMonths.hasNext();i++){
			char c;
			itoa(i+1,&c,10);
			args[0] = indentOffset;
			args[1] = UnicodeString(&c);
			args[2] = longMonths.getNextString(mError);
			xmlString.append(formatString(mStringsBundle.getStringEx("month",mError),args,3,t));
		}
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("monthNames",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}

void GenerateXML::writeMonthAbbr(UnicodeString& xmlString){

    ResourceBundle longMonths= mSourceBundle.get("MonthAbbreviations",mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING &&
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("monthAbbr",mError),args,2,t));
		indentOffset.append("\t");
		
		args[0] = indentOffset;
        for(int i=0;longMonths.hasNext();i++){
			char c;
			itoa(i+1,&c,10);
			args[1] = UnicodeString(&c);
			args[2] = longMonths.getNextString(mError);
			xmlString.append(formatString(mStringsBundle.getStringEx("month",mError),args,3,t));
		}
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("monthAbbr",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}



void GenerateXML::writeDayNames(UnicodeString& xmlString){
	ResourceBundle longDays= mSourceBundle.get("DayNames",mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("dayNames",mError),args,2,t));
		indentOffset.append("\t");
		args[0] = indentOffset;
		
		for(int i=0;longDays.hasNext();i++){
			args[1] = UnicodeString(getDayName(i));
			args[2] = longDays.getNextString(mError);
			xmlString.append(formatString(mStringsBundle.getStringEx("day",mError),args,3,t));
		}
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("dayNames",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}

void GenerateXML::writeDayAbbr(UnicodeString& xmlString){

	ResourceBundle abbrDays=mSourceBundle.get("DayAbbreviations", mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("dayAbbr",mError),args,2,t));
		indentOffset.append("\t");
		args[0] = indentOffset;
		
		for(int i=0;abbrDays.hasNext();i++){
			args[1] = UnicodeString(getDayName(i));
			args[2] = abbrDays.getNextString(mError);
			xmlString.append(formatString(mStringsBundle.getStringEx("day",mError),args,3,t));
		}
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("dayAbbr",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}
void GenerateXML::writeWeek(UnicodeString& xmlString){
	ResourceBundle dtElements = mSourceBundle.get("DateTimeElements", mError);
	int32_t len =0;
	const int32_t* vector = dtElements.getIntVector(len,mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("week",mError),args,2,t));
		indentOffset.append("\t");
		args[0] = indentOffset;
		args[1] = vector[0];
		xmlString.append(formatString(mStringsBundle.getStringEx("minDays",mError),args,2,t));
		args[1] = getDayName(vector[1]);
		xmlString.append(formatString(mStringsBundle.getStringEx("firstDay",mError),args,2,t));
		
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("week",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}

void GenerateXML::writeEra(UnicodeString& xmlString){

    ResourceBundle eras= mSourceBundle.get("Eras",mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		
		Formattable args[3] = {indentOffset,"",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("eras",mError),args,2,t));
		indentOffset.append("\t");
		
        args[0] = indentOffset;
        xmlString.append(formatString(mStringsBundle.getStringEx("eraAbbr",mError),args,2,t));

        indentOffset.append("\t");
		args[0] = indentOffset;
        for(int i=0;eras.hasNext();i++){
			char c;
			itoa(i+1,&c,10);
			args[1] = UnicodeString(&c);
			args[2] = eras.getNextString(mError);
			xmlString.append(formatString(mStringsBundle.getStringEx("era",mError),args,3,t));
		}
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("eraAbbr",mError),args,2,t));

        chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("eras",mError),args,2,t));
		mError=U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError =U_ZERO_ERROR;
}


void GenerateXML::writeAMPMmarkers(UnicodeString& xmlString){
	ResourceBundle markers =mSourceBundle.get("AmPmMarkers",mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
        U_SUCCESS(mError)){
		Formattable args[] = {indentOffset,markers.getStringEx((int32_t)0,mError)};
		xmlString.append(formatString(mStringsBundle.getStringEx("am",mError),args ,3,t));
		args[1] = markers.getStringEx((int32_t)1,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx("pm",mError),args ,3,t));
		mError= U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;

}

void GenerateXML::writeFormat(const char* style, const char* start, const char* end, const char* typeName,UnicodeString& pattern,
						      UnicodeString& xmlString, UBool split){
	UnicodeString t;
    UnicodeString type("type=\"");
    type.append(typeName);
    type.append("\"");
	if(!split){

		Formattable args[4]={indentOffset,"",type,""};
        if(*style!=0){
            xmlString.append(formatString(mStringsBundle.getStringEx(style,mError),args,3,t));


	        indentOffset.append("\t");
		    args[0]= indentOffset;
        }else{
            args[1] = type;
        }
		xmlString.append(formatString(mStringsBundle.getStringEx(start,mError),args,2,t));
		
		indentOffset.append("\t");
		args[0]=indentOffset;
		args[1]=pattern;
		xmlString.append(formatString(mStringsBundle.getStringEx("pattern",mError),args,2,t));
		

		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx(end, mError), indentOffset,t));
        
        if(*style!=0){
            chopIndent();
            args[0]=indentOffset;
            args[1]=UnicodeString(XML_END_SLASH);
            args[2]="";
            xmlString.append(formatString(mStringsBundle.getStringEx(style,mError),args,3,t));
        }

	}else{
		Formattable args[4]={indentOffset,"",type,""};
        if(*style!=0){
            xmlString.append(formatString(mStringsBundle.getStringEx(style,mError),args,3,t));

	        indentOffset.append("\t");
		    args[0]= indentOffset;
        }else{
            args[1]=type;
        }
		xmlString.append(formatString(mStringsBundle.getStringEx(start,mError),args,2,t));
		UnicodeString positive;
		UnicodeString negative;
		pattern.extractBetween(0,pattern.indexOf(";"),positive);
		pattern.extractBetween(pattern.indexOf(";")+1,pattern.length(),negative);
		indentOffset.append("\t");
		args[0]=indentOffset;
		if(!positive.isEmpty()){
			args[1]=positive;
			xmlString.append(formatString(mStringsBundle.getStringEx("pattern",mError),args,2,t));
		}
		if(!negative.isEmpty()){
			args[1]=negative;
			xmlString.append(formatString(mStringsBundle.getStringEx("pattern",mError),args,2,t));		
		}
		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx(end, mError), indentOffset,t));
        
        if(*style!=0){
            chopIndent();
            args[0]=indentOffset;
            args[1]=UnicodeString(XML_END_SLASH);
            args[2]="";
            xmlString.append(formatString(mStringsBundle.getStringEx(style,mError),args,3,t));
        }

    }
	
}

void GenerateXML::writeDateFormat(UnicodeString& xmlString){

	ResourceBundle dtPatterns = mSourceBundle.get("DateTimePatterns", mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
        U_SUCCESS(mError)){
		
		// hard code default to medium pattern for ICU
		Formattable args[4]= {indentOffset,"","",""};

		xmlString.append(formatString(mStringsBundle.getStringEx("dateFormats",mError),args,2,t));

		indentOffset.append("\t");
		args[0]= indentOffset;
		args[1] = "medium";
		xmlString.append(formatString(mStringsBundle.getStringEx( "default",mError),args,2,t));
		
		UnicodeString tempStr;
		
		writeFormat("dateFormatStyle","dateFormatStart","dateFormatEnd","full",dtPatterns.getStringEx((int32_t)4,mError),xmlString);
		writeFormat("dateFormatStyle","dateFormatStart","dateFormatEnd","long",dtPatterns.getStringEx((int32_t)5,mError),xmlString);
		writeFormat("dateFormatStyle","dateFormatStart","dateFormatEnd","medium",dtPatterns.getStringEx((int32_t)6,mError),xmlString);
		writeFormat("dateFormatStyle","dateFormatStart","dateFormatEnd","short",dtPatterns.getStringEx((int32_t)7,mError),xmlString);

		chopIndent();

		args[0]=indentOffset;
		args[1]=UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("dateFormats",mError),args,2,t));

  	}

	mError= U_ZERO_ERROR;
//	if(!print) xmlString.remove();	
}


void GenerateXML::writeTimeFormat(UnicodeString& xmlString){
	ResourceBundle dtPatterns = mSourceBundle.get("DateTimePatterns", mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
        U_SUCCESS(mError)){
		// hard code default to medium pattern for ICU
		Formattable args[4]= {indentOffset,"","",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("timeFormats",mError),args,2,t));
		
		indentOffset.append("\t");
		args[0]= indentOffset;
		args[1]="medium";
		xmlString.append(formatString(mStringsBundle.getStringEx( "default",mError),args,2,t));
		
		UnicodeString tempStr;
		
		writeFormat("","timeFormatStart","timeFormatEnd","full",dtPatterns.getStringEx((int32_t)0,mError),xmlString);
		writeFormat("","timeFormatStart","timeFormatEnd","long",dtPatterns.getStringEx((int32_t)1,mError),xmlString);
		writeFormat("","timeFormatStart","timeFormatEnd","medium",dtPatterns.getStringEx((int32_t)2,mError),xmlString);
		writeFormat("","timeFormatStart","timeFormatEnd","short",dtPatterns.getStringEx((int32_t)3,mError),xmlString);

		chopIndent();

		args[0]=indentOffset;
		args[1]=UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("timeFormats",mError),args,2,t));

	}

	mError= U_ZERO_ERROR;

//	if(!print) xmlString.remove();	
}

void GenerateXML::writeDateTimeFormat(UnicodeString& xmlString){
	ResourceBundle dtPatterns = mSourceBundle.get("DateTimePatterns", mError);
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
        U_SUCCESS(mError)){
		
		writeFormat("","dateTimeFormatStart","dateTimeFormatEnd","full",dtPatterns.getStringEx((int32_t)8,mError),xmlString);

	}

	mError= U_ZERO_ERROR;

//	if(!print) xmlString.remove();	
}

void GenerateXML::writeLocalePatternChars(UnicodeString& xmlString){

	UnicodeString temp=mSourceBundle.getStringEx("localPatternChars",mError);
	Formattable args[]={indentOffset,""};
	UnicodeString t;
	if(U_SUCCESS(mError) && mError!=U_USING_DEFAULT_WARNING && mError!=U_USING_FALLBACK_WARNING){
		args[1] = temp;
		xmlString.append(formatString(mStringsBundle.getStringEx( "localizedChars",mError),args,2,t));
		mError = U_ZERO_ERROR;
		return;
	}
	xmlString.remove();
	mError = U_ZERO_ERROR;
}

void GenerateXML::writeNumberFormat(){

	UnicodeString xmlString, tempStr;
	Formattable args[2]={indentOffset,""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx( "numbers",mError),args,2,t));
	indentOffset.append("\t");
	UBool print = FALSE;
	
	writeNumberElements(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeNumberPatterns(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}

	tempStr.remove();
	writeCurrencies(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	
	chopIndent();
	args[1]=UnicodeString(XML_END_SLASH);
	xmlString.append(formatString(mStringsBundle.getStringEx( "numbers",mError),args,2,t));
	if(print) printString(&xmlString);
}

void GenerateXML::writeNumberElements(UnicodeString& xmlString){

	DecimalFormatSymbols mySymbols(mLocale,mError);
	UnicodeString symbol;
	mError = U_ZERO_ERROR;
	ResourceBundle numFormats = mSourceBundle.get("NumberElements", mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError != U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		
		Formattable args[]={indentOffset,""};

		xmlString.append(formatString(mStringsBundle.getStringEx( "symbols",mError),args,2,t));
		
		indentOffset.append("\t");
        
		UnicodeString pattern= numFormats.getStringEx((int32_t)0,mError);
		
		args[0] = indentOffset;
		args[1] = pattern;
		
		xmlString.append(formatString(mStringsBundle.getStringEx( "decimal",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)1,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "group",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)2,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "list",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)3,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "percentSign",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)4,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "negativeZero",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)5,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "patternDigit",mError),args,2,t));
		
		args[1]="+";
		xmlString.append(formatString(mStringsBundle.getStringEx( "plusSign",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)6,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "minusSign",mError),args,2,t));
		
		args[1]=numFormats.getStringEx((int32_t)7,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx( "exponential",mError),args,2,t));
		
        symbol=mySymbols.getSymbol(DecimalFormatSymbols::kPerMillSymbol);
		args[1]=symbol;
		xmlString.append(formatString(mStringsBundle.getStringEx( "perMille",mError),args,2,t));
		
		symbol=mySymbols.getSymbol(DecimalFormatSymbols::kInfinitySymbol);
		args[1]=symbol;
		xmlString.append(formatString(mStringsBundle.getStringEx( "infinity",mError),args,2,t));

		symbol= mySymbols.getSymbol(DecimalFormatSymbols::kNaNSymbol);
		args[1]=symbol;//numFormats[11];
		xmlString.append(formatString(mStringsBundle.getStringEx( "nan",mError),args,2,t));
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx( "symbols",mError),args,2,t));
		return;

	}
	mError= U_ZERO_ERROR;
	xmlString.remove();
}


void GenerateXML::writeNumberPatterns(UnicodeString& xmlString){

	mError =U_ZERO_ERROR;
	ResourceBundle dtPatterns = mSourceBundle.get("NumberPatterns", mError);
	UnicodeString t;
	if( mError!=U_USING_DEFAULT_WARNING && 
		mError!=U_USING_FALLBACK_WARNING && 
        U_SUCCESS(mError)){
		// hard code default to medium pattern for ICU
		Formattable args[4]= {indentOffset,"","",""};
		xmlString.append(formatString(mStringsBundle.getStringEx("numberFormats",mError),args,2,t));
		indentOffset.append("\t");
		args[0]= indentOffset;
		args[1]="decimal";
		xmlString.append(formatString(mStringsBundle.getStringEx( "default",mError),args,2,t));

		UnicodeString tempStr;
		
		writeFormat("numberFormatStyle","numberFormatStart","numberFormatEnd","decimal",dtPatterns.getStringEx((int32_t)0,mError),xmlString,TRUE);
		writeFormat("numberFormatStyle","numberFormatStart","numberFormatEnd","currency",dtPatterns.getStringEx((int32_t)1,mError),xmlString,TRUE);
		writeFormat("numberFormatStyle","numberFormatStart","numberFormatEnd","percent",dtPatterns.getStringEx((int32_t)2,mError),xmlString,TRUE);
		writeFormat("numberFormatStyle","numberFormatStart","numberFormatEnd","scientific",dtPatterns.getStringEx((int32_t)3,mError),xmlString,TRUE);
		if(mError == U_MISSING_RESOURCE_ERROR){
			mError = U_ZERO_ERROR;
		}
		chopIndent();

		args[0]=indentOffset;
		args[1]=UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("numberFormats",mError),args,2,t));
        
	}

	mError= U_ZERO_ERROR;
}


void GenerateXML::writeCurrencies(UnicodeString& xmlString){

	UnicodeString tempStr;
	Formattable args[2] = { indentOffset,"" };
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("currencies",mError),args,2,t));
	indentOffset.append("\t");
	writeCurrency(tempStr);
	if(!tempStr.isEmpty()){

		xmlString.append(tempStr);
		chopIndent();
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
		xmlString.append(formatString(mStringsBundle.getStringEx("currencies",mError),args,2,t));
		//printString(&xmlString);
	}else{
		xmlString.remove();
	}
	chopIndent();
	mError= U_ZERO_ERROR;
}


void GenerateXML::writeCurrency(UnicodeString& xmlString){
	
	UBool isDefault =TRUE;
	ResourceBundle currency =mSourceBundle.get("CurrencyElements", mError);
    UnicodeString t;
	if( U_SUCCESS(mError) && 
		mError != U_USING_DEFAULT_WARNING && 
		mError != U_USING_FALLBACK_WARNING){
		
		Formattable args[] = {indentOffset,currency.getStringEx((int32_t)1,mError),""};
		xmlString.append(formatString(mStringsBundle.getStringEx( "default",mError),args,2,t));
		xmlString.append(formatString(mStringsBundle.getStringEx("currencyStart",mError),args,3,t));

		indentOffset.append("\t");
		
		args[0] = indentOffset;
		args[1] = currency.getStringEx((int32_t)0,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx("symbol",mError),args,2,t));

		args[1] = currency.getStringEx((int32_t)1,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx("displayName",mError),args,2,t));

		args[1] = currency.getStringEx((int32_t)2,mError);
		xmlString.append(formatString(mStringsBundle.getStringEx("decimal",mError),args,2,t));
/*
		NumberFormat* form = NumberFormat::createCurrencyInstance(mLocale,mError);
		if (form && mError!=U_USING_FALLBACK_WARNING) {
			UnicodeString pattern;
			pattern = ((DecimalFormat*)form)->toPattern(pattern);
            if(pattern.indexOf((UChar)';')<0){
                UChar temp[256] ={'\0'};
                pattern.extract(0,pattern.length()+1,temp,0);
                pattern.append(";-");
                pattern.append(temp);
            }
			args[1] = pattern;
			xmlString.append(formatString(UnicodeString(PTRN),args,2));
		}
*/
		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx("currencyEnd",mError),indentOffset,t));
		return;
	}
	xmlString.remove();
	mError=U_ZERO_ERROR;
}

void GenerateXML::writeSpecial(){
	UnicodeString xmlString, tempStr;
	Formattable args[]={indentOffset,"http://oss.software.ibm.com/icu/","",""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx( "special",mError),args,4,t));
	indentOffset.append("\t");
	UBool print = FALSE;
	
	writeBoundary(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeRuleBasedNumberFormat(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	chopIndent();
	args[1]=UnicodeString(XML_END_SLASH);
	xmlString.append(formatString(mStringsBundle.getStringEx( "specialEnd",mError),args,2,t));
	if(print) printString(&xmlString);
}

void GenerateXML::writeRuleBasedNumberFormat(UnicodeString& xmlString){
	UnicodeString tempStr;
	Formattable args[] = {indentOffset,"","",""};
	UBool print = FALSE;
	UnicodeString t;

    xmlString.append(formatString(mStringsBundle.getStringEx("ruleBasedNFS",mError),args,2,t));
	indentOffset.append("\t");

	UnicodeString spellout = mSourceBundle.getStringEx("SpelloutRules",mError);
	if( mError != U_USING_DEFAULT_WARNING && 
		U_SUCCESS(mError) &&
		mError != U_USING_FALLBACK_WARNING){
		escape(spellout);
		args[0] = indentOffset;
		args[1] = "spellout";
		args[2] = spellout;
		tempStr.append(formatString(mStringsBundle.getStringEx("ruleBasedNF",mError),args,3,t));

	}

	mError=U_ZERO_ERROR;
	UnicodeString ordinal = mSourceBundle.getStringEx("OrdinalRules", mError);
	if( mError != U_USING_DEFAULT_WARNING && 
		U_SUCCESS(mError) &&
		mError != U_USING_FALLBACK_WARNING){
		escape(ordinal);
		args[0] = indentOffset;
		args[1] = "ordinal";
		args[2] = ordinal;
		tempStr.append(formatString(mStringsBundle.getStringEx("ruleBasedNF",mError),args,3,t));
	}

	mError=U_ZERO_ERROR;
	UnicodeString duration = mSourceBundle.getStringEx("DurationRules", mError);
	if( mError != U_USING_DEFAULT_WARNING && 
		U_SUCCESS(mError) &&
		mError != U_USING_FALLBACK_WARNING){
		escape(duration);
		args[0] = indentOffset;
		args[1] = "duration";
		args[2] = duration;
		tempStr.append(formatString(mStringsBundle.getStringEx("ruleBasedNF",mError),args,3,t));
	}
	if(tempStr.isEmpty()){
		xmlString.remove();
	}else{
		chopIndent();
		xmlString.append(tempStr);
		args[0] = indentOffset;
		args[1] = UnicodeString(XML_END_SLASH);
	    xmlString.append(formatString(mStringsBundle.getStringEx("ruleBasedNFS",mError),args,2,t));
	}
	mError= U_ZERO_ERROR;
}
/*
void GenerateXML::writeTransliteration(){
	UnicodeString xmlString;
	const UnicodeString translit= mSourceBundle.getStringEx("TransliteratorNamePattern",mError);
	if(mError != U_USING_DEFAULT_WARNING && mError!=U_USING_FALLBACK_WARNING && U_SUCCESS(mError)){
		Formattable args[] = {indentOffset,translit};
		xmlString.append(formatString(UnicodeString(TL_NAME),args,2));
		printString(&xmlString);
	}
	mError=U_ZERO_ERROR;
}
*/
void GenerateXML::writeCharBrkRules(UnicodeString& xmlString){

    ResourceBundle brkRules = mSourceBundle.get("CharacterBreakRules",mError);
	UnicodeString t;
	if( mError != U_USING_DEFAULT_WARNING && 
		mError != U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		
		xmlString.append(formatString(mStringsBundle.getStringEx("graphemeStart",mError),indentOffset,t));
		indentOffset.append("\t");
        while(brkRules.hasNext()){
			UnicodeString rule =brkRules.getNextString(mError);
			escape(rule);
			xmlString.append("\n");
			xmlString.append(indentOffset);
			xmlString.append(rule);
		}
		xmlString.append("\n\n");
		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx("graphemeEnd",mError),indentOffset,t));
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;

}

void GenerateXML::writeSentBrkRules(UnicodeString& xmlString){

	ResourceBundle brkRules = mSourceBundle.get("SentenceBreakRules",mError);
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("sentenceStart",mError),indentOffset,t));
	if( mError != U_USING_DEFAULT_WARNING && 
		mError != U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
		indentOffset.append("\t");
        while(brkRules.hasNext()){
			UnicodeString rule =brkRules.getNextString(mError);
			escape(rule);
			xmlString.append("\n");
			xmlString.append(indentOffset);
			xmlString.append(rule);
		}
		xmlString.append("\n\n");
		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx("graphemeEnd",mError),indentOffset,t));
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;

}
void GenerateXML::writeLineBrkRules(UnicodeString& xmlString){

	ResourceBundle brkRules = mSourceBundle.get("LineBreakRules",mError);
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("lineStart",mError),indentOffset,t));

	if( mError != U_USING_DEFAULT_WARNING && 
		mError != U_USING_FALLBACK_WARNING && 
		U_SUCCESS(mError)){
	   indentOffset.append("\t");
       while(brkRules.hasNext()){
			UnicodeString rule =brkRules.getNextString(mError);
			escape(rule);
			xmlString.append("\n");
			xmlString.append(indentOffset);
			xmlString.append(rule);
		}
		xmlString.append("\n\n");
		chopIndent();
		xmlString.append(formatString(mStringsBundle.getStringEx("lineEnd",mError),indentOffset,t));
		return;
	}
	xmlString.remove();
	mError= U_ZERO_ERROR;
}

void GenerateXML::writeBoundary(UnicodeString& xmlString){
	UnicodeString tempStr;
	UBool print=FALSE;
	Formattable args[3] = { indentOffset,"",""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("boundaries",mError),args,2,t));
	indentOffset.append("\t");
	
	writeCharBrkRules(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeSentBrkRules(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeLineBrkRules(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	chopIndent();
	xmlString.append(formatString(mStringsBundle.getStringEx("boundaries",mError),args,2,t));

	if(!print) xmlString.remove();
}


void GenerateXML::writeCollations(){
	UnicodeString tempStr,xmlString;
	UBool print=FALSE;
	Formattable args[3] = { indentOffset,"",""};
	UnicodeString t;
	xmlString.append(formatString(mStringsBundle.getStringEx("collations",mError),args,2,t));
	indentOffset.append("\t");
	
	writeCollation(mSourceBundle,tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	
	UnicodeString locale = mLocale.getLanguage();
	locale.append("__");
	char localeName[256] ;
	for(int i = 0; i<sizeof(collationVariants)/sizeof(char*);i++){
		UnicodeString var(collationVariants[i],2,strlen(collationVariants[i]));
		UnicodeString loc(locale);
		loc.append(var);
		loc.extract(0,loc.length(),localeName,sizeof(localeName));
		ResourceBundle bundle(path,localeName ,mError);
		if( U_SUCCESS(mError) && 
			mError != U_USING_FALLBACK_WARNING && 
			mError != U_USING_DEFAULT_WARNING){
			writeCollation(bundle,tempStr,&var);
			if(!tempStr.isEmpty()){
				xmlString.append(tempStr);
				print = TRUE;
			}
			tempStr.remove();
		}else{
			mError = U_ZERO_ERROR;
		}
	}

	/*writeSentBrkRules(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}
	tempStr.remove();
	writeLineBrkRules(tempStr);
	if(!tempStr.isEmpty()){
		xmlString.append(tempStr);
		print = TRUE;
	}*/
	chopIndent();
	args[1]=UnicodeString(XML_END_SLASH);
	xmlString.append(formatString(mStringsBundle.getStringEx("collations",mError),args,2,t));
	
	if(print)  printString(&xmlString);
	
}
void GenerateXML::writeCollation(ResourceBundle& bundle,UnicodeString& xmlString, UnicodeString* collKey){
    char* key="CollationElements";
    UnicodeString version;
    UnicodeString overide="FALSE";
    UnicodeString sequence;
    UnicodeString rules;
    ResourceBundle dBundle=bundle.get(key,mError);
	UnicodeString t;
    if( mError!=U_USING_DEFAULT_WARNING && 
		U_SUCCESS(mError) && 
		mError!=U_USING_FALLBACK_WARNING){
        Formattable args[]={indentOffset,"","","",""};
		if(collKey!=NULL){
			UnicodeString str = "key=\"";
			str.append(*collKey);
			str.append("\"");
			args[2] = str;
		}
		xmlString.append(formatString(mStringsBundle.getStringEx("collation",mError),args,3,t));

		indentOffset.append("\t");

		while(dBundle.hasNext()){
            ResourceBundle dBundle1 = dBundle.getNext(mError);
            const char* mykey=dBundle1.getKey();
            if(stricmp(mykey,"Version")==0){
               version = dBundle.getStringEx(mykey,mError);
			   UnicodeString temp = UnicodeString("version=\"").append(version);
			   temp.append("\"");
			   args[0] = indentOffset;
			   args[1] = "http://oss.software.ibm.com/icu/";
			   args[2] = temp;
			   args[3] = UnicodeString(XML_END_SLASH);
			   xmlString.append(formatString(mStringsBundle.getStringEx("special",mError),args,4,t));
            }else if(stricmp(mykey,"Sequence")==0){
                sequence = dBundle.getStringEx(mykey,mError);
				rules = parseRules((UChar*)sequence.getBuffer(),sequence.length(),rules);
				xmlString.append(rules);
                sequence.releaseBuffer();
           }else if(stricmp(mykey,"Override")==0){
                overide =  dBundle.getStringEx(mykey,mError);
            }
        }
        
        chopIndent();
		args[0] = indentOffset;
		args[1] = "/";
		args[2] ="";
		xmlString.append(formatString(mStringsBundle.getStringEx("collation",mError),args,3,t));
        //printString(&xmlString);
    }
    mError = U_ZERO_ERROR;
}

#define UCOL_TOK_UNSET    0xFFFFFFFF
#define UCOL_TOK_RESET    0xDEADBEEF
#define UCOL_TOK_SETTING  0xFFFFFFFE
#define UCOL_TOK_OVERRIDE 0xFFFFFFFD 

UBool 
uprv_isRuleWhiteSpace(UChar32 c) {
    /* "white space" in the sense of ICU rule parsers: Cf+White_Space */
    return
        u_charType(c)==U_FORMAT_CHAR ||
        u_hasBinaryProperty(c, UCHAR_WHITE_SPACE);
}
/*
alternate		non-ignorable
				shifted		[alternate non-ignorable]	alternate="non-ignorable" 
backwards		on
				off			[backwards on]				backwards="on" 
normalization	on
				off			[normalization on]			normalization="off" 
caseLevel		on
				off			[caseLevel on]				caseLevel="off" 
caseFirst		upper
				lower
				off			[caseFirst off]				caseFirst="off" 
hiraganaQ		on
				off			[hiraganaQ on]				hiraganaQuarternary="on" 
strength		primary 
				secondary 
				tertiary 
				quarternary 
				identical   [strength 1]				strength="primary" 
*/

void GenerateXML::writeSettings(UnicodeString& src , UnicodeString& xmlString){
	const UChar* start = src.getBuffer();
	const UChar* limit = start + src.length(); 
	UChar setting[256];
	UChar value[20];
	int32_t valueLen=0,settingLen =0;
	UBool isValue = FALSE;
	Formattable args[] = {indentOffset,setting,value};
	UnicodeString t;
	if(src.indexOf("suppressContractions") >=0 || src.indexOf("optimize") >= 0){
		int dCount=0, bCount = 0;
		while(start < limit){
			UChar ch = *start++;
			if((ch == 0x5b && bCount == 0)){ // skip the first '[' 
				bCount++;
				continue;
			}
			if(ch == 0x5b){
				isValue = TRUE;
			}
			if(ch == 0x5d && start==limit){
				continue;
			}
			if(isValue){
				value[valueLen++]=ch;
			}
		}
		args[1] = UnicodeString(value,valueLen);
		if(src.indexOf("optimize") >= 0){
			xmlString.append(formatString(mStringsBundle.getStringEx("optimize",mError),args,2,t));
		}else{
			xmlString.append(formatString(mStringsBundle.getStringEx("suppressContractions",mError),args,2,t));	
		}
	}else{
		while(start < limit ){
			UChar ch = *start++;
			if(uprv_isRuleWhiteSpace(ch)){
				if(settingLen>0){
					isValue = TRUE;
				}
				continue; //skip white spaces
			}else if(ch == 0x5b ){ // skip '[' && ']'
				continue;
			}else if(ch == 0x5d){ // ']'
				UnicodeString set(setting,settingLen);
				if(set.indexOf("hiraganaQ")>=0){
					args[1] = UnicodeString("hiraganaQuarternary");
				}else if(set.indexOf("casefirst")>=0){
					args[1] = UnicodeString("caseFirst");
				}else{
					args[1] = set;
				}
				args[2] = UnicodeString(value,valueLen);
				xmlString.append(formatString(mStringsBundle.getStringEx("settings",mError),args,3,t));
				valueLen = settingLen =0;
				isValue =FALSE;
				continue;
			}
			if(isValue){
				value[valueLen++] = ch;
			}else{
				setting[settingLen++] = ch;
			}
		}
	}

	src.releaseBuffer();
}

void GenerateXML::writeReset(UnicodeString& src, UnicodeString& xmlString){
	const UChar* start = src.getBuffer();
	const UChar* limit = start + src.length(); 
	UChar setting[256];
	UChar value[20];
	UChar chars[20];
	UnicodeString t;
	char* target;
	int32_t valueLen=0,settingLen =0, charsLen=0;
	UBool isValue = FALSE, isChars=FALSE;
	if(src.indexOf("top")>=0){
		Formattable args[] = {indentOffset, mStringsBundle.getStringEx("top",mError)};
		xmlString.append(formatString(mStringsBundle.getStringEx("reset",mError),args,2,t));
	}else if(src.indexOf("[")>=0 && src.length() > 1){
		while(start < limit ){
			UChar ch = *start++;
			if(uprv_isRuleWhiteSpace(ch)){
				if(settingLen>0){
					isValue = TRUE;
				}else if(valueLen>0){
					isChars = TRUE;
				}
				continue; //skip white spaces
			}else if(ch == 0x5b || ch == 0x5d ){ // skip '[' && ']'
				continue;
			}
			if(isValue){
				value[valueLen++] = ch;
			}else if(isChars){
				chars[charsLen++] =ch;
			}else{
				setting[settingLen++] = ch;
			}
		}
		switch(value[0]){
		case 0x31:
			target = "primary";
			break;
		case 0x32:
			target = "secondary";
			break;
		case 0x33:
			target = "tertiary";
			break;
		case 0x34:
			target = "quaternary";
			break;
		default:
			target = "unknown";
			break;
		}
		Formattable args[] = {indentOffset, UnicodeString(setting,settingLen), UnicodeString(target), UnicodeString(chars,charsLen)};
		xmlString.append(formatString(mStringsBundle.getStringEx("resetWithValue",mError),args,4,t));
	}else{
		Formattable args[] = {indentOffset, src};
		xmlString.append(formatString(mStringsBundle.getStringEx("reset",mError),args,2,t));
	}
}
UnicodeString GenerateXML::parseRules(UChar* rules, int32_t ruleLen, UnicodeString& xmlString){
	Token src;
	src.start = rules;
	src.current = rules;
	src.prevCurrent = rules;
	src.end = rules+ruleLen;
	src.chars=(UChar*) malloc((ruleLen + 10)* U_SIZEOF_UCHAR);
	src.charsCapacity = ruleLen+10;
	UnicodeString collStr ;
	uint32_t prevStrength=UCOL_DEFAULT;
	int32_t count = 0;
	UBool appendedRules = FALSE;
    UnicodeString t;
	UBool startOfRules = TRUE;
	if(src.start != src.end){
		for(;;){
			 
			  uint32_t strength = parseRules(&src,startOfRules);
			  UnicodeString tempStr;
			  startOfRules = FALSE;
			  tempStr.append(src.chars,src.charsLen);
			  escape(tempStr);
			  if(tempStr.indexOf((UChar)0x7c) >= 0){
					tempStr.findAndReplace((UChar)0x7C, mStringsBundle.getStringEx("context",mError));
					tempStr.append(mStringsBundle.getStringEx("contextEnd",mError));
			  }
			  if((prevStrength != strength) || (prevStrength==strength && src.current >= src.end)){
					char* singleKey = NULL;
					char* seqKey = NULL;
					
					// assume that settings always preceed rule strings

					Formattable args[] = {indentOffset,collStr,""};
					if(prevStrength != UCOL_DEFAULT){
						if(prevStrength == UCOL_TOK_SETTING){
							writeSettings(collStr,xmlString);
						}else if(prevStrength==UCOL_TOK_RESET){
							if(appendedRules == FALSE){
								args[0] = indentOffset;
								args[1] = "";
								xmlString.append(formatString(mStringsBundle.getStringEx("rules",mError),args,2,t));
								indentOffset.append("\t");
								appendedRules = TRUE;
							}
							writeReset(collStr,xmlString);
						}else if(prevStrength==UCOL_TOK_OVERRIDE){
							args[1]="backwards";
							args[2]= "2";
							xmlString.append(formatString(mStringsBundle.getStringEx("settings",mError),args,3,t));
						}else{
							if(appendedRules == FALSE){
								args[0] = indentOffset;
								args[1] = "";
								xmlString.append(formatString(mStringsBundle.getStringEx("rules",mError),args,2,t));
								indentOffset.append("\t");
								appendedRules = TRUE;
							}
							switch(prevStrength){
							case UCOL_IDENTICAL:
								singleKey = "identical";
								seqKey = "identicalSeq";
								break;
							case UCOL_PRIMARY:
								singleKey = "primary";
								seqKey = "primarySeq";
								break;
							case UCOL_SECONDARY:
								singleKey = "secondary";
								seqKey = "secondarySeq";
								break;
							case UCOL_TERTIARY:
								singleKey = "tertiary";
								seqKey = "tertiarySeq";
								break;
							case UCOL_QUATERNARY:
								singleKey = "quaternary";
								seqKey = "quaternarySeq";
								break;

							}
							if(count <= 1){
								xmlString.append(formatString(mStringsBundle.getStringEx(singleKey,mError),args,2,t));
							}else{
								xmlString.append(formatString(mStringsBundle.getStringEx(seqKey,mError),args,2,t));
							}
						}
						if(src.current == src.end){
							break;
						}
						//reset
						count = 0;
						collStr.remove();
					}
			  }
			  collStr.append(tempStr);
			  count++;
			  prevStrength = strength;		
		}
	}
	if(appendedRules==TRUE){
		chopIndent();
		Formattable args[]= {indentOffset,UnicodeString(XML_END_SLASH)};
		xmlString.append(formatString(mStringsBundle.getStringEx("rules",mError),args,2,t));
	}

	free(src.chars);
	return xmlString;
}
void GenerateXML::escape(UnicodeString& str){
	UnicodeString temp;
    UChar test[10] = {'\0'};
	Formattable args[] = {indentOffset,""};
	UnicodeString t;
	for(int i=0;i<str.length();i++){
        UChar c = str.charAt(i);
        switch(c){
        case '<':
			temp.append("&lt;");
            break;
        case '>':
			temp.append("&gt;");
            break;
        case '&':
			temp.append("&amp;");
            break;
        case '"':
            temp.append("&quot;");
		    break;
		case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
        case 0x04:
        case 0x05:
        case 0x06:
        case 0x07:
        case 0x08:
        case 0x09:
        case 0x0A:
        case 0x0b:
        case 0x0c:
        case 0x0D:
        case 0x0e:
        case 0x0f:
        case 0x10:
        case 0x11:
        case 0x12:
        case 0x13:
        case 0x14:
        case 0x15:
        case 0x16:
        case 0x17:
        case 0x18:
        case 0x19:
        case 0x1A:
        case 0x1b:
        case 0x1c:
        case 0x1d:
        case 0x1e:
        case 0x1f:
            itou(test,c,16,4);  
			args[1] = UnicodeString(test); 
            temp.append(formatString(mStringsBundle.getStringEx("cp",mError),args,2,t));
            break;
        default:
			temp.append(str.charAt(i));
        }
        
	}
	str=temp;
}


static
int32_t u_strncmpNoCase(const UChar     *s1, 
     const UChar     *s2, 
     int32_t     n) 
{
    if(n > 0) {
        int32_t rc;
        for(;;) {
            rc = (int32_t)u_tolower(*s1) - (int32_t)u_tolower(*s2);
            if(rc != 0 || *s1 == 0 || --n == 0) {
                return rc;
            }
            ++s1;
            ++s2;
        }
    }
    return 0;
}

UnicodeString& appendHex(uint32_t number,
            int32_t digits,
            UnicodeString& target)
{
    static const UChar digitString[] = {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0
    }; /* "0123456789ABCDEF" */

    switch (digits)
    {
    case 8:
        target += digitString[(number >> 28) & 0xF];
    case 7:
        target += digitString[(number >> 24) & 0xF];
    case 6:
        target += digitString[(number >> 20) & 0xF];
    case 5:
        target += digitString[(number >> 16) & 0xF];
    case 4:
        target += digitString[(number >> 12) & 0xF];
    case 3:
        target += digitString[(number >>  8) & 0xF];
    case 2:
        target += digitString[(number >>  4) & 0xF];
    case 1:
        target += digitString[(number >>  0) & 0xF];
        break;
    default:
        target += "**";
    }
    return target;
}

// Replace nonprintable characters with unicode escapes
UnicodeString& prettify(const UnicodeString &source,UnicodeString &target)
{
    int32_t i;

    target.remove();
    target += "\"";

    for (i = 0; i < source.length(); )
    {
        UChar32 ch = source.char32At(i);
        i += UTF_CHAR_LENGTH(ch);

        if (ch < 0x09 || (ch > 0x0A && ch < 0x20)|| ch > 0x7E)
        {
            if (ch <= 0xFFFF) {
                target += "\\u";
                appendHex(ch, 4, target);
            } else {
                target += "\\U";
                appendHex(ch, 8, target);
            }
        }
        else
        {
            target += ch;
        }
    }

    target += "\"";

    return target;
}

static inline 
void syntaxError(const UChar* rules, 
                 int32_t pos,
                 int32_t rulesLen,
                 UParseError* parseError) {
    parseError->offset = pos;
    parseError->line = 0 ; /* we are not using line numbers */
    
    // for pre-context
    int32_t start = (pos <=U_PARSE_CONTEXT_LEN)? 0 : (pos - (U_PARSE_CONTEXT_LEN-1));
    int32_t stop  = pos;
    
    u_memcpy(parseError->preContext,rules+start,stop-start);
    //null terminate the buffer
    parseError->preContext[stop-start] = 0;
    
    //for post-context
    start = pos+1;
    stop  = ((pos+U_PARSE_CONTEXT_LEN)<= rulesLen )? (pos+(U_PARSE_CONTEXT_LEN-1)) : 
                                                            u_strlen(rules);

    u_memcpy(parseError->postContext,rules+start,stop-start);
    //null terminate the buffer
    parseError->postContext[stop-start]= 0;

    UnicodeString preCon,postCon;
	prettify(UnicodeString(parseError->preContext),preCon);
	prettify(parseError->postContext,postCon);
	char preChar[256], postChar[256];
	preCon.extract(0,preCon.length(),preChar,sizeof(preChar));
	postCon.extract(0,postCon.length(),postChar,sizeof(postChar));
	printf("parseRules() failed. Pre-context: %s \t post-context: %s \n",preChar, postChar);
	exit(-1);
}

#define ucol_tok_isSpecialChar(ch)              \
    (((((ch) <= 0x002F) && ((ch) > 0x0020)) || \
      (((ch) <= 0x003F) && ((ch) >= 0x003A)) || \
      /* (((ch) <= 0x0060) && ((ch) >= 0x005B)) || \ */ \
	  (((ch) == 0x0060) ||    ((ch) == 0x005c) || ((ch) == 0x005e) || ((ch) == 0x005f) ) || \
      (((ch) <= 0x007E) && ((ch) >= 0x007D)) || \
      (ch) == 0x007B))



void GenerateXML::growBuffer(UChar* src, int32_t len, int32_t size, int32_t requiredCapacity, UErrorCode* status){
	UChar* temp =NULL;
	
	if(status ==NULL || U_FAILURE(*status)){
		return;
	}
	if(requiredCapacity < len ){
		*status = U_ILLEGAL_ARGUMENT_ERROR;
		return;
	}
	temp = (UChar*) malloc ( size * requiredCapacity);
	if(temp ==NULL){
		*status = U_MEMORY_ALLOCATION_ERROR;
		return;
	}
	memmove(temp,src,len*size);
	free(src);
	src = temp;
}

/*
 *
 *  "&		a	<<		befg	<<		c		<<		d		<<		d"
 *   ^			^				^										^
 *   start		prevCurrent		current 								end
 */

uint32_t GenerateXML::parseRules(Token* src,UBool startOfRules){
  /* parsing part */
  UBool variableTop = FALSE;
  //UBool top = FALSE;
  UBool inChars = TRUE;
  UBool inQuote = FALSE;
  UBool wasInQuote = FALSE;
  UChar *optionEnd = NULL;
  uint8_t before = 0;
  UBool isEscaped = FALSE;
  UParseError parseError;
  uint32_t newExtensionLen = 0;
  uint32_t extensionOffset = 0;
  uint32_t newStrength = UCOL_TOK_UNSET; 
  UBool isSetting = FALSE;
  const UChar top[] = {0x005b,0x0074,0x006f,0x0070,0x005d};

  src->prevCurrent = src->current;
  src->charsLen = 0;

  while (src->current < src->end) {
    UChar ch = *(src->current);


    if (inQuote) {
      if (ch == 0x0027/*'\''*/) {
          inQuote = FALSE;
      }else{
		  if(src->charsLen >= (src->charsCapacity-1)){
			  src->charsCapacity*=2;
			 growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
		  }
		  src->chars[src->charsLen++] =ch;
	  }
    }else if(isEscaped){
      isEscaped =FALSE;
      if (newStrength == UCOL_TOK_UNSET) {
		mError = U_INVALID_FORMAT_ERROR;
        syntaxError(src->start,(int32_t)(src->current-src->start),(int32_t)(src->end-src->start),&parseError);
        return NULL;
        // enabling rules to start with non-tokens a < b
		// newStrength = UCOL_TOK_RESET;
      }

    }else if(isSetting==TRUE ||!uprv_isRuleWhiteSpace(ch)) {
        /* Sets the strength for this entry */
        switch (ch) {
          case 0x003D/*'='*/ : 
            if (newStrength != UCOL_TOK_UNSET) {
              goto EndOfLoop;
            }

            /* if we start with strength, we'll reset to top */
            if(startOfRules == TRUE) {
              newStrength = UCOL_TOK_RESET;
			  u_strcpy(src->chars+src->charsLen,top);
			  src->charsLen+=u_strlen(top);
              goto EndOfLoop;
            }
            newStrength = UCOL_IDENTICAL;
            break;

          case 0x002C/*','*/:  
            if (newStrength != UCOL_TOK_UNSET) {
              goto EndOfLoop;
            }

            /* if we start with strength, we'll reset to top */
            if(startOfRules == TRUE) {
              newStrength = UCOL_TOK_RESET;
			  u_strcpy(src->chars+src->charsLen,top);
			  src->charsLen+=u_strlen(top);
              goto EndOfLoop;
            }
            newStrength = UCOL_TERTIARY;
            break;
		  
		  case 0x002D: /* - add to src->chars and continue */
			  if(src->charsLen >= (src->charsCapacity-1)){
				src->charsCapacity*=2;
				growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
			  }
			  src->chars[src->charsLen++] = ch;
			  break;
          case  0x003B/*';'*/:
            if (newStrength != UCOL_TOK_UNSET) {
              goto EndOfLoop;
            }

            /* if we start with strength, we'll reset to top */
            if(startOfRules == TRUE) {
			  u_strcpy(src->chars+src->charsLen,top);
			  src->charsLen+=u_strlen(top);
              newStrength = UCOL_TOK_RESET;
              goto EndOfLoop;
            }
            newStrength = UCOL_SECONDARY;
            break;

          case 0x003C/*'<'*/:  
            if (newStrength != UCOL_TOK_UNSET) {
              goto EndOfLoop;
            }

            /* if we start with strength, we'll reset to top */
            if(startOfRules == TRUE) {
			  u_strcpy(src->chars+src->charsLen,top);
			  src->charsLen+=u_strlen(top);
              newStrength = UCOL_TOK_RESET;
              goto EndOfLoop;
            }
            /* before this, do a scan to verify whether this is */
            /* another strength */
            if(*(src->current+1) == 0x003C) {
              src->current++;
              if(*(src->current+1) == 0x003C) {
                src->current++; /* three in a row! */
                newStrength = UCOL_TERTIARY;
              } else { /* two in a row */
                newStrength = UCOL_SECONDARY;
              }
            } else { /* just one */
              newStrength = UCOL_PRIMARY;
            }
            break;

          case 0x0026/*'&'*/:  
            if (newStrength != UCOL_TOK_UNSET) {
              /**/
              goto EndOfLoop;
            }

            newStrength = UCOL_TOK_RESET; /* PatternEntry::RESET = 0 */
            break;

		  case 0x005B:
			  if (newStrength == UCOL_TOK_UNSET){
				  newStrength = UCOL_TOK_SETTING;
			  }
			  if(!inQuote){
				  isSetting=TRUE;
			  }
			  if(src->charsLen >= (src->charsCapacity-1)){
				src->charsCapacity*=2;
				growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
			  }
			  src->chars[src->charsLen++] =ch;
			  break;
		  case 0x005D:
			  if(isSetting==TRUE){
				  isSetting = FALSE;
			  }
			  if(src->charsLen >= (src->charsCapacity-1)){
				src->charsCapacity*=2;
				growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
			  }
			  src->chars[src->charsLen++] = ch;
			  break;
		  case 0x0021/*! skip java thai modifier reordering*/:
			  break; 
          case 0x002F/*'/'*/:
            wasInQuote = FALSE; /* if we were copying source characters, we want to stop now */
            inChars = FALSE; /* we're now processing expansion */
            break;
          case 0x005C /* back slash for escaped chars */:
              isEscaped = TRUE;
              break;
          /* found a quote, we're gonna start copying */
          case 0x0027/*'\''*/:
            if (newStrength == UCOL_TOK_UNSET) { /* quote is illegal until we have a strength */
			  mError = U_INVALID_FORMAT_ERROR;
              syntaxError(src->start,(int32_t)(src->current-src->start),(int32_t)(src->end-src->start),&parseError);
              return NULL;
			  // enabling rules to start with a non-token character a < b
              // newStrength = UCOL_TOK_RESET;
            }
			if(inQuote==FALSE){
				inQuote = TRUE;
				wasInQuote = TRUE;
			}else{
				inQuote =FALSE;
				wasInQuote = FALSE;
			}
			// removed inChars

            

            ch = *(++(src->current)); 
            if(ch == 0x0027) { /* copy the double quote */
              //*src->extraCurrent++ = ch;
			  if(src->charsLen >= (src->charsCapacity-1)){
				 src->charsCapacity*=2;
			     growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
			  }
			  src->chars[src->charsLen++] = ch;
			  if(*(src->current+1)!=0x0027){
				inQuote = FALSE;
			  }
            }else{
				--src->current;
			}
            break;

          /* '@' is french only if the strength is not currently set */
          /* if it is, it's just a regular character in collation rules */
          case 0x0040/*'@'*/:
            if (newStrength == UCOL_TOK_UNSET) {
              //src->opts->frenchCollation = UCOL_ON;
			  //french secondary
			  newStrength = UCOL_TOK_OVERRIDE;
              break;
            }

          case 0x007C /*|*/: /* this means we have actually been reading prefix part */
            // we want to store read characters to the prefix part and continue reading
            // the characters (proper way would be to restart reading the chars, but in
            // that case we would have to complicate the token hasher, which I do not 
            // intend to play with. Instead, we will do prefixes when prefixes are due
            // (before adding the elements).

			//wasInQuote = TRUE;
			if(src->charsLen >= (src->charsCapacity-1)){
			  src->charsCapacity*=2;
			  growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);
			}
			src->chars[src->charsLen++]=ch;
            //ch = *(++(src->current)); 
            break;
          
            //charsOffset = 0;
            //newCharsLen = 0;
            //break; // We want to store the whole prefix/character sequence. If we break
                     // the '|' is going to get lost.
          default:
            if (newStrength == UCOL_TOK_UNSET) {
			  mError = U_INVALID_FORMAT_ERROR;
              syntaxError(src->start,(int32_t)(src->current-src->start),(int32_t)(src->end-src->start),&parseError);
              return NULL;
            }

            if (ucol_tok_isSpecialChar(ch) && (inQuote == FALSE)) {
              mError = U_INVALID_FORMAT_ERROR;
              syntaxError(src->start,(int32_t)(src->current-src->start),(int32_t)(src->end-src->start),&parseError);
              return NULL;
            }
			if(src->charsLen >= (src->charsCapacity-1)){
			 src->charsCapacity*=2;
			 growBuffer(src->chars,src->charsLen,U_SIZEOF_UCHAR, src->charsCapacity,&mError);

			}
			src->chars[src->charsLen++] =ch;
            if(ch == 0x0000 && src->current+1 == src->end) {
              break;
            }

            break;
          }         
       }
	   src->current++;
    }



 EndOfLoop:
  wasInQuote = FALSE;
  if (newStrength == UCOL_TOK_UNSET) {
    return NULL;
  }

  return newStrength;
}
/*
void GenerateXML::writePosixCompData(){
    char temp[50]={'\0'};
    strcpy(temp,locName);
    strcat(temp,"_PCD");
    Locale loc(temp);
    ResourceBundle bundle(path, loc,mError);
    if(mError==U_ZERO_ERROR){
        UnicodeString xmlString;
        xmlString.append(formatString(UnicodeString(POSIX_START),indentOffset));
        indentOffset.append("\t");
        
        writeMessages(bundle,xmlString);
        addressFormat(bundle,xmlString);
        nameFormat(bundle, xmlString);
        identity(bundle, xmlString);
        telephoneFormat( bundle, xmlString);
        
        chopIndent();
        xmlString.append(formatString(UnicodeString(POSIX_END), indentOffset));
        printString(&xmlString);

    }
    
    
}

void GenerateXML::writeMessages(ResourceBundle& bundle, UnicodeString& xmlString){
    UnicodeString temp,temp1;
    ResourceBundle dBundle = bundle.get("Messages",mError);
    if(U_SUCCESS(mError)){
        temp.append(formatString(UnicodeString(MSG_START),indentOffset));
        indentOffset.append("\t");
        getStringRes("yesExpression",dBundle,temp1,UnicodeString(YES));
        getStringRes("noExpression",dBundle,temp1, UnicodeString(NO));
        if(temp1.length()!=0){
            temp.append(temp1);
            temp.append(formatString(UnicodeString(MSG_END),indentOffset));
        }else{
            temp.remove();
        }
        chopIndent();
        xmlString.append(temp);
    }

}

void GenerateXML::addressFormat(ResourceBundle& bundle,UnicodeString& xmlString){
    UnicodeString temp,temp1;
    ResourceBundle dBundle = bundle.get("AddressFormat",mError);
    if(U_SUCCESS(mError)){
        temp.append(formatString(UnicodeString(ADDR_START),indentOffset));
        indentOffset.append("\t");
        getStringRes("PostalFormat",dBundle,temp1,UnicodeString(POSTAL));
        if(temp1.length()!=0){
            temp.append(temp1);
            temp.append(formatString(UnicodeString(ADDR_END),indentOffset));
        }else{
            temp.remove();
        }
        chopIndent();
        xmlString.append(temp);
    }
}

void GenerateXML::nameFormat(ResourceBundle& bundle,UnicodeString& xmlString){
    UnicodeString temp,temp1;
    ResourceBundle dBundle = bundle.get("NameFormat",mError);
    if(U_SUCCESS(mError)){
        temp.append(formatString(UnicodeString(NF_START),indentOffset));
        indentOffset.append("\t");

        getStringRes("NamePattern",dBundle,temp1,UnicodeString(NAME_PAT));
        getStringRes("GeneralSalutaion",dBundle,temp1, UnicodeString(GEN_SALUT));
        getStringRes("ShortSalutationMr",dBundle,temp1, UnicodeString(SH_SALUT_MR));
        getStringRes("ShortSalutationMiss",dBundle,temp1, UnicodeString(SH_SALUT_MS));
        getStringRes("ShortSalutationMrs",dBundle,temp1, UnicodeString(SH_SALUT_MI));
        getStringRes("LongSalutationMr",dBundle,temp1, UnicodeString(LG_SALUT_MR));
        getStringRes("LongSalutationMiss",dBundle,temp1, UnicodeString(LG_SALUT_MS));
        getStringRes("LongSalutationMrs",dBundle,temp1, UnicodeString(LG_SALUT_MI));

        if(temp1.length()!=0){
            temp.append(temp1);
            temp.append(formatString(UnicodeString(NF_END),indentOffset));
        }else{
            temp.remove();
        }
        chopIndent();
        xmlString.append(temp);
    }
}
	
void GenerateXML::identity(ResourceBundle& bundle,UnicodeString& xmlString){
	    UnicodeString temp,temp1;
	    ResourceBundle dBundle = bundle.get("Identification",mError);
		if(U_SUCCESS(mError)){
	        temp.append(formatString(UnicodeString(ID_START),indentOffset));
        indentOffset.append("\t");

        getStringRes("Title",dBundle,temp1,UnicodeString(TITLE));
        getStringRes("Source",dBundle,temp1, UnicodeString(SOURCE));
        getStringRes("Address",dBundle,temp1, UnicodeString(ADDR_1));
       // getStringRes("Contact",dBundle,temp1, UnicodeString(CONTACT));
        getStringRes("Email",dBundle,temp1, UnicodeString(EMAIL));
        getStringRes("Telephone",dBundle,temp1, UnicodeString(TELEPH));
        getStringRes("Fax",dBundle,temp1, UnicodeString(FAX));
        getStringRes("u",dBundle,temp1, UnicodeString(LANG_1));
        getStringRes("Territory",dBundle,temp1,UnicodeString(TRTRY));
        getStringRes("Audience",dBundle,temp1, UnicodeString(AUDIENCE));
        getStringRes("Application",dBundle,temp1, UnicodeString(APPLIC));
        getStringRes("Abbreviation",dBundle,temp1, UnicodeString(ABBR_1));
        getStringRes("Revision",dBundle,temp1, UnicodeString(REV));
        getStringRes("Date",dBundle,temp1, UnicodeString(DATE_1));

        if(temp1.length()!=0){
            temp.append(temp1);
            temp.append(formatString(UnicodeString(ID_END),indentOffset));
        }else{
            temp.remove();
        }
        chopIndent();
        xmlString.append(temp);
    }
}


void GenerateXML::telephoneFormat(ResourceBundle& bundle,UnicodeString& xmlString){
    UnicodeString temp,temp1;
    ResourceBundle dBundle = bundle.get("TelephoneFormat",mError);
    if(U_SUCCESS(mError)){
        temp.append(formatString(UnicodeString(TF_START),indentOffset));
        indentOffset.append("\t");

        getStringRes("InternationalFormat",dBundle,temp1, UnicodeString(IP_TF));
        getStringRes("DomesticFormat",dBundle,temp1, UnicodeString(DP_TF));
        getStringRes("InternationalDialCode",dBundle,temp1, UnicodeString(IDC_TF));
        getStringRes("InternationalPrefix",dBundle,temp1, UnicodeString(IPF_TF));

        if(temp1.length()!=0){
            temp.append(temp1);
            temp.append(formatString(UnicodeString(TF_END),indentOffset));
        }else{
            temp.remove();
        }
        chopIndent();
        xmlString.append(temp);
    }
}


void GenerateXML::writePosixAdditions(){
    UnicodeString xmlString;
    writeMeasurement(xmlString);
    writeCountryPost(xmlString);
    writeCountryCar(xmlString);
    writeCountryISBNNumber(xmlString);
    writeLanguageLibraryUse(xmlString);
    writePaperSize(xmlString);
    if(xmlString.length()>0){
        printString(&xmlString);
    }
}

void GenerateXML::writePaperSize(UnicodeString& xmlString){
    UnicodeString temp;
    ResourceBundle dBundle = mSourceBundle.get("PaperSize", mError);
    if(U_SUCCESS(mError)){
        indentOffset.append("\t");
        getStringRes("Height",temp,UnicodeString(HEIGHT));
        getStringRes("Width",temp,UnicodeString(WIDTH));
        getStringRes("Units",temp,UnicodeString(UNITS));
        chopIndent();
        if(temp.length()>0){
            xmlString.append(formatString(UnicodeString(PAPER),indentOffset));
            xmlString.append(temp);
            xmlString.append(formatString(UnicodeString(PAPER_END), indentOffset));
        }      
    }
    mError=U_ZERO_ERROR;
}

void GenerateXML::writeMeasurement(UnicodeString& xmlString){
    UnicodeString temp;
    getStringRes("Measurement",temp,UnicodeString(MEASURE));
    if(temp.length()>0){
        xmlString.append(temp);
    }
}
void GenerateXML::writeCountryPost(UnicodeString& xmlString){
    UnicodeString temp;
    getStringRes("CountryPost",temp,UnicodeString(POST));
    if(temp.length()>0){
        xmlString.append(temp);
    }
}
void GenerateXML::writeCountryCar(UnicodeString& xmlString){
    
    UnicodeString temp;
    getStringRes("CountryCar",temp,UnicodeString(CNTRY_CAR));
    if(temp.length()>0){
        xmlString.append(temp);
    }
}
void GenerateXML::writeCountryISBNNumber(UnicodeString& xmlString){
    
    UnicodeString temp;
    getStringRes("CountryISBN",temp,UnicodeString(ISBN_NUM));
    if(temp.length()>0){
        xmlString.append(temp);
    }
}
void GenerateXML::writeLanguageLibraryUse(UnicodeString& xmlString){
    UnicodeString temp;
    getStringRes("LanguageLibUse",temp,UnicodeString(LANG_LIB));
    if(temp.length()>0){
        xmlString.append(temp);
    }
}

void GenerateXML::getStringRes(const char *key,ResourceBundle& bundle,UnicodeString& xmlString,UnicodeString pattern){
    ResourceBundle myBundle = mSourceBundle;
    mSourceBundle = bundle;
    getStringRes(key,xmlString,pattern);
    mSourceBundle = myBundle;
}
void GenerateXML::getStringRes(const char* key,UnicodeString& xmlString,UnicodeString pattern){
    UnicodeString temp=mSourceBundle.getStringEx(key,mError);
	Formattable args[]={indentOffset,""};
	if(!U_FAILURE(mError)){
		args[1] = temp;
		xmlString.append(formatString(pattern,args,2));
	}
	mError = U_ZERO_ERROR;
}

*/