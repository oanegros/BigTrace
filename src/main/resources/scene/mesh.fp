out vec4 fragColor;

uniform vec4 colorin;
in vec3 Normal;
in vec3 FragPos;

//const vec3 ObjectColor = vec3(1, 1, 1);


//const vec3 lightColor1 = 0.5 * vec3(0.9, 0.9, 1);
const vec3 lightColor1 = vec3(1.0, 1.0, 1.0);
const vec3 lightDir1 = normalize(vec3(0, -0.2, -1));

const vec3 lightColor2 = 0.5 * vec3(0.1, 0.1, 1);
const vec3 lightDir2 = normalize(vec3(1, 1, 0.5));

const vec3 ambient = vec3(0.1, 0.1, 0.1);

const float specularStrength = 1;

vec3 phong(vec3 norm, vec3 viewDir, vec3 lightDir, vec3 lightColor, float shininess, float specularStrength)
{
	float diff = max(dot(norm, lightDir), 0.0);
	vec3 diffuse = diff * lightColor;

	vec3 reflectDir = reflect(-lightDir, norm);
	float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
	vec3 specular = specularStrength * spec * lightColor;

	return diffuse + specular;
}

void main()
{

		vec3 norm = normalize(Normal);
		vec3 viewDir = normalize(-FragPos);
	
		vec3 l1 = phong( norm, viewDir, lightDir1, lightColor1, 32, 0.1 );
		vec3 l2 = phong( norm, viewDir, lightDir2, lightColor2, 32, 0.5 );
		//float alphax = min(1.0, 1.0-abs(dot(norm,viewDir)));
		//float alphax = min(1.0, cos(3.14*0.5*abs(dot(norm,viewDir))));
		float alphax = min(1.0, 1.0-pow(abs(dot(norm,viewDir)),1.0));

	
		//fragColor = vec4((ambient + l1 + l2) * colorin.rgb, colorin.a);
		//fragColor = vec4((ambient + l1 ) * colorin.rgb, colorin.a);
		 
		
		fragColor = vec4(colorin.rgb, colorin.a*alphax);
		gl_FragDepth = 1.0;
	//}
}
