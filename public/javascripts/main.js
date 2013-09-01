var app = angular.module('UserConfigApp', []);

app.controller('LoginController', ['$scope', '$http', function($scope, $http){
	$scope.isVisible = {
			"loginBox" : true,
			"oauthAuthorizeBox" : false
	};
	$scope.getUser = function() {
		requestData = {"email" : $scope.email};
		$http.post('/users/login', requestData)
		.success(function(responseData){
			$scope.nodes = responseData;
			$scope.isVisible.loginBox = false;
			$scope.isVisible.oauthAuthorizeBox = true;
		}).error(function(errorData){
			$scope.isVisible.loginBox = true;
			$scope.isVisible.oauthAuthorizeBox = false;
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
