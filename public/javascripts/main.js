var app = angular.module('CreateProcessApp', []);

app.controller('ProcessController', ['$scope', '$http', function($scope, $http){
	
	$scope.isVisible = {
		"trigger" : false,
		"action" : false,
		"step2" : false,
		"step3" : false
	};

	
	$http.get('/nodes/all')
	.success(function(responseData){
		$scope.nodes = responseData;
	}).error(function(errorData){
		$scope.error = errorData;
	});
	
	
	$scope.updateTriggers = function() {
		$http.get('/nodes/reflection/' + $scope.triggerNode.nodeId)
		.success(function(responseData){
			$scope.triggers = responseData.triggers;
			$scope.trigger = $scope.triggers[0];
			$scope.isVisible.trigger = true;
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}

	
	$scope.updateActions = function() {
		$http.get('/nodes/reflection/' + $scope.actionNode.nodeId)
		.success(function(responseData){
			$scope.actions = responseData.actions;
			$scope.action = $scope.actions[0];
			$scope.isVisible.action = true;
		}).error(function(errorData){
			$scope.error = errorData;
		});
	}
	
	
	// If both action and trigger are set, then make Step 2 visible
	$scope.$watch('action + trigger', function(){
		if($scope.action && $scope.trigger) {
			$scope.isVisible.step2 = true;
			$scope.isVisible.step3 = true;
		}
	});

}]);
