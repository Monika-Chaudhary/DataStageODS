def call(compName, applicationProcess, envName, auth, deployServer, versionId, UCDProcess){
    sh'''
    export JAVA_HOME=/pathTo/JDK
    workspace=$PWD
    export PATH=$PATH:/path/toUdclient
    export  auth='''+auth+'''
    export  deployServer='''+deployServer+'''
    export  applicationProcess='''+applicationProcess+'''
    export  compName='''+compName+'''
    export  envName='''+envName+'''
    export  versionId='''+versionId+'''
    
    if [ "$envName" == "Pre-Prod" || "$envName" == "Production" ];then
        sourceFolder=$WORKSPACE/DownloadArtifact/
    else
        sourceFolder=$WORKSPACE/DataStage-TAR/
    fi
    
    echo "TAR Source Folder : $sourceFolder"
    
    echo -e "{" > compProp1.json
    echo -e '"component": "'$compName'",' >> compProp1.json
    echo -e '"name": "deployServer",' >> compProp1.json
    echo -e '"value": "'$deployServer'",' >> compProp1.json
    echo -e "}" > compProp1.json
    /path/toUdclient/udclient -weburl ucdUrl -authtoken $auth setComponentProperty compProp1.json
    
    #Put userId & password to run datatsage script in UCD comp property(pwd must be secure in UCD)
    
    echo -e "{" > compProp1.json
    echo -e '"component": "'$compName'",' >> compProp1.json
    echo -e '"name": "deployServer",' >> compProp1.json
    echo -e '"value": "'$deployServer'",' >> compProp1.json
    echo -e "}" > compProp1.json
    /path/toUdclient/udclient -weburl ucdUrl -authtoken $auth setComponentProperty compProp1.json
    
    #create version for component in UCD
    /path/toUdclient/udclient -weburl ucdUrl -authtoken $auth createVersion -component $compName -name $versionId -description $compName
    echo 'Created comp version'
    
    #upload version files for comp
    /path/toUdclient/udclient -weburl ucdUrl -authtoken $auth -component $compName -version $versionId -base "$sourceFolder"
    
    displayEnvName=$envName
    
    #deploy to $envName environment
    echo -e "{" > req.Application.json
    echo -e '"application": "ucdApplicationName",' >> req.Application.json
    echo -e '"description": "Deploying desired TAR through Jenkins & UCD",' >> req.Application.json
    echo -e '"applicationProcess": "'$applicationProcess'",' >> req.Application.json
    echo -e '"environment": "'$envName'",' >> req.Application.json
    echo -e '"onlyChanged": "false",' >> req.Application.json
    echo -e '"versions": [{' >> req.Application.json
    echo -e '"version": "'$versionId'",' >> req.Application.json
    echo -e '"component": "'$compName'",' >> req.Application.json
    echo -e "}]": >> req.Application.json
    echo -e "}" >> req.Application.json

    #invoke deployment & get request id
    request_id=$(echo $(udclient -weburl ucdUrl -authtoken $auth requestApplicationProcess - $workspace/req.Application.json) | jq "requestId" | sed 's/"//g')
    echo 'Resuest Id:'
    echo $request_id

    #function to check request id status
    getProcessStatus(){
        udclient -weburl ucdUrl -authtoken $auth getApplicationProcessRequestStatus -request  $request_id > response.json 2?&1
        grep CLOSED response.json || exit_code=$?
        if [ "$exit_code" == "" ]; then
            udclient -weburl ucdUrl -authtoken $auth getApplicationProcessExecution -request  $request_id
            if grep CLOSED response.json && grep FAULTED response.json;then
                error "Failed at 'Deploy to $envName environment' step, exiting now...."
                UCDProcess="FAILURE"
                exit 1
            fi
        else
            sleep 30; unset exit_code
            getProcessStatus
        fi
    }
    #call function to check request id status
    getProcessStatus
    fi
    '''
}
