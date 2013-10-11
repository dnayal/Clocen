var allprocesses = angular.module('AllProcessesApp', []);

allprocesses.controller('AllProcessController', ['$scope', '$http', function($scope, $http){
	
	$scope.processes = [];
	
	$scope.getAllProcesses = function() {
		$http.get('/process/all')
		.success(function(responseData){
			$scope.processes = responseData;
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
	
	$scope.deleteProcess = function(processId) {
		$http.get('/process/delete/'+processId)
		.success(function(responseData){
			$scope.getAllProcesses();
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
}]);


var allprocesses = angular.module('ViewProcessApp', []);

allprocesses.controller('ProcessController', ['$scope', '$http', function($scope, $http){
	
	$scope.process = [];
	
	$scope.getProcess = function(processId) {
		$http.get('/process/'+processId)
		.success(function(responseData){
			$scope.process = responseData;
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}

}]);

