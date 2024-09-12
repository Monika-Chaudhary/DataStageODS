#Put this file under DevShared Library

def call(projectName, DEPLOYTYPE, DSVERSION, deployServer){
    sh'''
    #!/bin/bash
    
    #set all values
    export  projectName='''+projectName+'''
    export  DEPLOYTYPE='''+DEPLOYTYPE+'''
    export  DSVERSION='''+DSVERSION+'''
    export  deployServer='''+deployServer+'''
    listfile=dsx_list.log
    logfile=deploy_log.log
    
    #projectName=${projectName//\r/}  #remove carriage return
    #DEPLOYTYPE=${DEPLOYTYPE//\r/} 
    #DSVERSION=${DSVERSION//\r/}
    
    folder=datastageODS
    
    #Remove directory if exist
    if [ -d "$folder" ];then
        rm -rf "$folder"
    fi
    
    #Create directory if don't exist
    if [ ! -d "$folder" ];then
        mkdir "$folder"
    fi
    
    tempFolder="$folder/tempFolder"
    
    echo "Folder: $folder"
    echo "tempFolder: $tempFolder"
    
    #Create temp directory if don't exist
    if [ ! -d "$tempFolder" ];then
        mkdir "$tempFolder"
    fi
    
    #LogFile
    echo $(date) > "$folder/$logfile"
    
    #Put required files intp datastageODS folder from git repo
    dsx_list=$(grep -E -v '^[[:space:]]*$|^#.*' $WORKSPACE/config_file.txt)  #remove space from this file
    echo "$dsx_list" > "$folder/$listfile"
    dlist=$(cat "$folder/$listfile")
    echo "all list items: $dlist"
    
    chmod -R 755 "$folder"
    
    #count items in listFile and copy required files to Deployment
    listCount=0
    for line in $dlist
    do
        echo -e "list item : $line\n"
        line=${line//\r/}
        listCount=$((listCount+1))
        echo "list count : $listCount"
        findFile=$(find $WORKSPACE -name "$line")
        echo "file found here : $findFile"
        if [ -n "findFile" ];then
            for reqFiles in "$findFile"
            do
                echo "required file : $reqFiles"
                ext="${reqFiles##*.}"
                filename=$(basename "$reqFiles" ."$ext")
                echo "filename: $filename and ext of file: $ext"
                cp "$reqFiles" "$tempFolder"
                reqPath=$(echo "$reqFiles" | sed "s,$WORKSPACE,,g")    #remove WORKSPACE path from required file to get required path only
                echo "req Path: $reqPath"
                folderN=$(echo "reqPath" | sed "s,$filename.$ext,,g")  #taking only file folderName (/DataStage_CodeFolder/)
                modified="${folderN:1:-1}"    #remove 1st and last char from folderN (eg. DataStage_Code Folder)
                echo "folderN: $folderN"
                echo "modified: $modified"
                if [ ! -d "$folder/$modified" ];then
                    mkdir "$folder/$modified"
                fi
                mv "$tempFolder/$filename.$ext" "$folder/$modified"
            done
        fi
    done
    
    #log and TAR creation start
    echo "***Going to start Deployment to $DEPLOYTYPE***"
    echo "***Information for doing Deployment***"
    echo "Deployment Type: $DEPLOYTYPE"
    echo "Project Name: $projectName"
    echo "DSVERSION: $DSVERSION"
    echo "deployServer: $deployServer"
    echo "Input List: $listfile containing $listCount items"
    
    echo "***Information for doing Deployment***" >> "$folder/$logfile"
    echo "Deployment Type: $DEPLOYTYPE" >> "$folder/$logfile"
    echo "Project Name: $projectName" >> "$folder/$logfile"
    echo "DSVERSION: $DSVERSION" >> "$folder/$logfile"
    echo "deployServer: $deployServer" >> "$folder/$logfile"
    echo "Input List: $listfile containing $listCount items" >> "$folder/$logfile"
    
    rmdir "$tempFolder"
    
    tarFolder=DataStage-TAR
    
    #Remove tar directory if exist
    if [ -d "$tarFolder" ];then
        rm -rf "$tarFolder"
    fi
    
    #Create if tar directory don't exist
    if [ ! -d "$tarFolder" ];then
        mkdir "$tarFolder"
    fi
    
    cd "$folder"
    tar --exclude='*.log' -czf $WORKSPACE/"$tarFolder"/dataStageODS.tar.gz .
    chmod -R 755 $WORKSPACE/"$tarFolder"
    '''
}
