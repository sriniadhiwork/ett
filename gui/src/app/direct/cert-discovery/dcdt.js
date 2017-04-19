var ccdaValidator = angular.module('ttt.direct.dcdtValidator', []);

ccdaValidator.controller('DCDTValidatorCtrl', ['$scope', 'DCDTValidatorFactory', '$state', 'ApiUrl','$http','CCDADocumentsFactory',
	function($scope, DCDTValidatorFactory, $state, ApiUrl,$http,CCDADocumentsFactory) {
	$scope.pageTitle= $state.current.data.pageTitle;
		$scope.fileInfo = {
			"flowChunkNumber": "",
			"flowChunkSize": "",
			"flowCurrentChunkSize": "",
			"flowTotalSize": "",
			"flowIdentifier": "",
			"flowFilename": "",
			"flowRelativePath": "",
			"flowTotalChunks": ""
		};


    $scope.data = [
      {name:"Hosting allows a System Under Test (SUT) to verify that their certificates are hosted correctly, and discoverable by other Direct implementations.", hreflink:"panel_hosting",children:0},
      {name:"Discovery allows a SUT to verify that they can discover certificates in other Direct implementations by using them to send Direct messages.",  hreflink:"panel_discovery",children:1}
    ];

 $scope.directAddress ="";
$scope.discoveryTestCase = [
    { code: "", name: "--No testcase selected--" },
    { code: "D1_DNS_AB_Valid", name: "D1 - Valid address-bound certificate discovery in DNS",
Binding_Type: "ADDRESS",
Location_Type: "DNS",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's DNS can host and return the expected address-bound X.509 certificate.",
RTM_Sections: "1, 3",
RFC_4398:  "Section 2.1",
Direct_SHT: "Section 5.3",
Instructions: "Enter a Direct address corresponding to an address-bound X.509 certificate that is hosted by your system's DNS and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen.",
Target_Certificate: [{"label": "C-CDA Document Type",
"name": "ccdaReferenceFilename",
"datatype": "CCDAWidget",
"value": "ccdaReferenceFilename",
"readOnly": false,
"display": true
}],
Background_Certificate: [{"label": "C-CDA Document Type",
"name": "ccdaReferenceFilename",
"datatype": "CCDAWidget",
"value": "ccdaReferenceFilename",
"readOnly": false,
location1:[{"label": "C-CDA Document Type",
"name": "ccdaReferenceFilename",
"datatype": "CCDAWidget",
"value": "ccdaReferenceFilename",
"readOnly": false,
"display": true
}]
}]
},
{ code: "D2_DNS_DB_Valid", name: "D2 - Valid domain-bound certificate discovery in DNS",
Binding_Type: "DOMAIN",
Location_Type: "DNS",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's DNS can host and return the expected domain-bound X.509 certificate.",
RTM_Sections: "1, 3",
RFC_4398:  "Section 2.1",
Direct_SHT: "Section 5.3",
Instructions: "Enter a Direct address corresponding to a domain-bound X.509 certificate that is hosted by your system's DNS and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
},{ code: "D3_LDAP_AB_Valid", name: "D3 - Valid address-bound certificate discovery in LDAP",
Binding_Type: "ADDRESS",
Location_Type: "LDAP",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's LDAP server can host and return the expected address-bound X.509 certificate.",
RTM_Sections: "2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22",
RFC_2798:  "Section 9.1.2",
Instructions: "Enter a Direct address corresponding to an address-bound X.509 certificate that is hosted by your system's LDAP server and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
},{ code: "D4_LDAP_DB_Valid", name: "D4 - Valid domain-bound certificate discovery in LDAP",
Binding_Type: "DOMAIN",
Location_Type: "LDAP",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's LDAP server can host and return the expected domain-bound X.509 certificate.",
RTM_Sections: "2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22",
RFC_2798:  "Section 9.1.2",
Instructions: "Enter a Direct address corresponding to a domain-bound X.509 certificate that is hosted by your system's LDAP server and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
}];


$scope.processes= [
    { code: "", name: "--No testcase selected--" },
    { code: "H1_DNS_AB_Normal", name: "H1 - Normal address-bound certificate search in DNS",
Binding_Type: "ADDRESS",
Location_Type: "DNS",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's DNS can host and return the expected address-bound X.509 certificate.",
RTM_Sections: "1, 3",
RFC_4398:  "Section 2.1",
Direct_SHT: "Section 5.3",
Instructions: "Enter a Direct address corresponding to an address-bound X.509 certificate that is hosted by your system's DNS and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
},
{ code: "H2_DNS_DB_Normal", name: "H2 - Normal domain-bound certificate search in DNS",
Binding_Type: "DOMAIN",
Location_Type: "DNS",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's DNS can host and return the expected domain-bound X.509 certificate.",
RTM_Sections: "1, 3",
RFC_4398:  "Section 2.1",
Direct_SHT: "Section 5.3",
Instructions: "Enter a Direct address corresponding to a domain-bound X.509 certificate that is hosted by your system's DNS and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
},
{ code: "H3_LDAP_AB_Normal", name: "H3 - Normal address-bound certificate search in LDAP",
Binding_Type: "ADDRESS",
Location_Type: "LDAP",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's LDAP server can host and return the expected address-bound X.509 certificate.",
RTM_Sections: "2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22",
RFC_2798:  "Section 9.1.2",
Instructions: "Enter a Direct address corresponding to an address-bound X.509 certificate that is hosted by your system's LDAP server and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
},
{ code: "H4_LDAP_DB_Normal", name: "H4 - Normal domain-bound certificate search in LDAP",
Binding_Type: "DOMAIN",
Location_Type: "LDAP",
Negative: "false",
Optional: "false",
Description: "This test case verifies that your system's LDAP server can host and return the expected domain-bound X.509 certificate.",
RTM_Sections: "2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22",
RFC_2798:  "Section 9.1.2",
Instructions: "Enter a Direct address corresponding to a domain-bound X.509 certificate that is hosted by your system's LDAP server and then click Submit. DCDT will attempt to discover the certificate and display the result on the screen."
}];


	$scope.selectedItem = $scope.processes[0];
	$scope.discorySelectedItem = $scope.discoveryTestCase[0];

   $scope.onSelectionChange= function(selectedItem,testcase) {
      console.log(" selectedItem...... "+angular.toJson(selectedItem,true));
      if (testcase === "process"){
           $scope.dcdtResult = selectedItem;
      }else{
           $scope.dcdtDiscoveryResult = selectedItem;
      }
   };


      $scope.hostingProcess = function() {
		console.log("hostingProcess......"+hostingProcess);
      };

			var headers = {
				'Access-Control-Allow-Origin' : '*',
				'Access-Control-Allow-Methods' : 'POST, GET, OPTIONS, PUT',
				'Content-Type': 'application/json',
				'Accept': 'application/json'
			};



$scope.sendData = function() {
			$http({
				method: 'POST',
				url: 'http://dcdt31prod.sitenv.org:8080/dcdt/hosting/process',
				headers: headers,
				data: {"@type": "hostingTestcaseSubmission",
				"directAddr":"provider1@direct.sitenv.org",
				"testcase": "H1_DNS_AB_Normal"}
			})
			.success(function(data, status, header) {
				console.log('sucess');
			})
			.error(function(data, status, header) {
				console.log('error');
			});
		};

        $scope.validator = {
            "@type": "hostingTestcaseSubmission",
            "directAddr": "provider1@direct.sitenv.org",
            "testcase": "H1_DNS_AB_Normal"
        };
        $scope.restdata = function() {
			console.log("hostingProcess......"+ $scope.directAddress );
             $scope.dcdtResult = null;
             $scope.selectedItem = $scope.processes[0];
             $scope.directAddress ="";
        };

        $scope.validate = function() {
            $scope.laddaLoading = true;
                    DCDTValidatorFactory.save($scope.validator, function(data) {
                       console.log(" $scope.data dcdt::::"+ angular.toJson(data,true));
                       $scope.dcdtResult = data;
                     }, function(data) {
                        $scope.laddaLoading = false;
                        throw {
                            code: data.data.code,
                            url: data.data.url,
                            message: data.data.message
                        };
                    });
        };

        $scope.ccdaSenderData = {};
        CCDADocumentsFactory.get(function(data) {
          $scope.ccdaDocuments = data;
            if (data !== null) {
                $scope.ccdaSenderData = $scope.ccdaDocuments[Object.keys(data)[0]];
            }
        });

     $scope.apiUrl = ApiUrl.get();

    }
]);
