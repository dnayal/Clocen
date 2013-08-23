var app = angular.module('UserConfigApp', []);

app.controller('LoginController', ['$scope', '$http', function($scope, $http){
	$scope.isVisible = {
			"loginBox" : true,
			"oauthAuthorizeBox" : false
	};
	$scope.getUser = function() {
		requestData = {"email" : $scope.email};
		$http.post('/users/find/email', requestData)
		.success(function(responseData){
			console.log("Success");
			$scope.user = {"name" : responseData.name};
			$scope.isVisible.loginBox = false;
		}).error(function(errorData){
			console.log("Error Message");
			$scope.isVisible.loginBox = true;
		});
	}
}]);


app.directive('myappCurrentTime', function($timeout, dateFilter){
	return function(scope, element, attrs) {
		var format;
		scope.format = "d-MMM-yyyy hh:mm:ss a";
		
		function updateTime() {
			element.text(dateFilter(new Date(), format))
		}
		
		scope.$watch(attrs.myappCurrentTime, function(value){
			format = value;
			updateTime();
		});
		
		setInterval(function(){updateTime();}, 1000);
	}
});
