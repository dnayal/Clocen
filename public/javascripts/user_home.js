var app = angular.module('UserHomeApp', []);

app.controller('ProcessController', ['$scope', '$http', function($scope, $http){
	
	$scope.process = [];
	
	$http.get('/process/all')
	.success(function(responseData){
		$scope.process = responseData;
	}).error(function(errorData){
		$scope.error = errorData;
	});
	
}]);
